package com.android.identity.issuance.simple

import com.android.identity.issuance.evidence.EvidenceRequest
import com.android.identity.issuance.evidence.EvidenceRequestIcaoNfcTunnel
import com.android.identity.issuance.evidence.EvidenceRequestIcaoNfcTunnelType
import com.android.identity.issuance.evidence.EvidenceRequestIcaoPassiveAuthentication
import com.android.identity.issuance.evidence.EvidenceRequestMessage
import com.android.identity.issuance.evidence.EvidenceRequestQuestionMultipleChoice
import com.android.identity.issuance.evidence.EvidenceRequestQuestionString
import com.android.identity.issuance.evidence.EvidenceResponse
import com.android.identity.issuance.evidence.EvidenceResponseIcaoNfcTunnelResult
import com.android.identity.issuance.evidence.EvidenceResponseQuestionMultipleChoice

/**
 * A builder of the graph of [Node]s that describes proofing workflows.
 *
 * A chain of steps without forks or merges can be described using DSL inside
 * [SimpleIssuingAuthorityProofingGraph.create] call scope.
 *
 * Each node in the graph has associated id which can be used to look up [EvidenceResponse]
 * associated with this node in [SimpleIssuingAuthority.credentialGetConfiguration] and
 * [SimpleIssuingAuthority.checkEvidence] method implementations.
 */
class SimpleIssuingAuthorityProofingGraph {
    private val chain = mutableListOf<(Node?) -> Node>()

    companion object {
        fun create(init: SimpleIssuingAuthorityProofingGraph.() -> Unit): Node {
            val graph = SimpleIssuingAuthorityProofingGraph()
            graph.init()
            val first = graph.build(null)
            return first ?: throw IllegalStateException("No nodes were added")
        }
    }

    /** Sends [EvidenceRequestMessage]. */
    fun message(id: String, message: String, acceptButtonText: String, rejectButtonText: String?) {
        val evidenceRequest = EvidenceRequestMessage(message, acceptButtonText, rejectButtonText)
        chain.add { followUp -> SimpleNode(id, followUp, evidenceRequest) }
    }

    /** Sends [EvidenceRequestQuestionString]. */
    fun question(id: String, message: String, defaultValue: String, acceptButtonText: String) {
        val evidenceRequest = EvidenceRequestQuestionString(message, defaultValue, acceptButtonText)
        chain.add { followUp -> SimpleNode(id, followUp, evidenceRequest) }
    }

    /**
     * Sends [EvidenceRequestQuestionMultipleChoice].
     *
     * Branches can be configured using [Choices.on] calls.
     */
    fun choice(id: String, message: String, acceptButtonText: String, initChoices: Choices.() -> Unit) {
        val choices = Choices()
        choices.initChoices()
        val request = EvidenceRequestQuestionMultipleChoice(message, choices.choices, acceptButtonText)
        chain.add { followUp ->
            MultipleChoiceNode(id, request, choices.graphs.mapValues { graph ->
                graph.value.build(followUp)
            })
        }
    }

    /**
     * Sends [EvidenceRequestIcaoPassiveAuthentication].
     */
    fun icaoPassiveAuthentication(id: String, dataGroups: List<Int>) {
        val evidenceRequest = EvidenceRequestIcaoPassiveAuthentication(dataGroups)
        chain.add { followUp -> SimpleNode(id, followUp, evidenceRequest) }
    }

    /**
     * Sends [EvidenceRequestQuestionMultipleChoice].
     *
     * Branches can be configured using [IcaoChoices] methods.
     */
    fun icaoTunnel(id: String, dataGroups: List<Int>, initChoices: IcaoChoices.() -> Unit) {
        chain.add { followUp ->
            val choices = IcaoChoices()
            choices.initChoices()
            val map = setOf(choices.noAuthenticationGraph, choices.activeAuthenticationGraph,
                choices.chipAuthenticationGraph).associateBy({graph -> graph}) { graph ->
                    graph.build(followUp)
                }
            IcaoNfcTunnelNode(id, dataGroups,
                successfulActiveAuthentication = map[choices.activeAuthenticationGraph]!!,
                successfulChipAuthentication = map[choices.chipAuthenticationGraph]!!,
                noAuthentication = map[choices.noAuthenticationGraph]!!
            )
        }
    }

    class Choices {
        val graphs = mutableMapOf<String, SimpleIssuingAuthorityProofingGraph>()
        val choices = mutableMapOf<String, String>()

        fun on(id: String, text: String, init: SimpleIssuingAuthorityProofingGraph.() -> Unit) {
            choices[id] = text
            val graph = SimpleIssuingAuthorityProofingGraph()
            graphs[id] = graph
            graph.init()
        }
    }

    class IcaoChoices {
        val activeAuthenticationGraph = SimpleIssuingAuthorityProofingGraph()
        var chipAuthenticationGraph = SimpleIssuingAuthorityProofingGraph()
        val noAuthenticationGraph = SimpleIssuingAuthorityProofingGraph()

        /**
         * Configures the branch that should be used when some form of authentication succeeded
         * (either Active Authentication or Chip Authentication).
         */
        fun whenAuthenticated(init: SimpleIssuingAuthorityProofingGraph.() -> Unit) {
            activeAuthenticationGraph.init()
            chipAuthenticationGraph = activeAuthenticationGraph
        }

        /**
         * Configures the branch that should be used when Chip Authentication succeeded.
         */
        fun whenChipAuthenticated(init: SimpleIssuingAuthorityProofingGraph.() -> Unit) {
            chipAuthenticationGraph.init()
        }

        /**
         * Configures the branch that should be used when Active Authentication succeeded.
         */
        fun whenActiveAuthenticated(init: SimpleIssuingAuthorityProofingGraph.() -> Unit) {
            activeAuthenticationGraph.init()
        }

        /**
         * Configures the branch that should be used when MRTD does not support any authentication
         * method that can verify that MRTD was not cloned.
         */
        fun whenNotAuthenticated(init: SimpleIssuingAuthorityProofingGraph.() -> Unit) {
            noAuthenticationGraph.init()
        }
    }

    private fun build(followUp: Node?): Node? {
        var node: Node? = followUp
        for (i in chain.indices.reversed()) {
            node = chain[i](node)
        }
        return node
    }

    abstract class Node {
        abstract val nodeId: String
        abstract val requests: List<EvidenceRequest>
        abstract val followUps: Iterable<Node>  // graph is finite and walkable
        open fun selectFollowUp(response: EvidenceResponse): Node? {
            val iterator = followUps.iterator()
            if (iterator.hasNext()) {
                val followUp = iterator.next()
                if (iterator.hasNext()) {
                    throw IllegalStateException(
                        "When there are multiple follow-ups, selectFollowUp must be implemented")
                }
                return followUp;
            }
            return null;
        }
    }

    class SimpleNode(
        override val nodeId: String,
        private val followUp: Node?,
        private val request: EvidenceRequest): Node() {

        override val requests: List<EvidenceRequest>
            get() = listOf(request)
        override val followUps: Iterable<Node>
            get() = if (followUp == null) { listOf<Node>() } else { listOf(followUp) }
    }

    class MultipleChoiceNode(
        override val nodeId: String,
        private val request: EvidenceRequestQuestionMultipleChoice,
        private val followUpMap: Map<String, Node?>): Node() {

        override val requests: List<EvidenceRequest>
            get() = listOf(request)
        override val followUps: Iterable<Node>
            get() = followUpMap.values.filterNotNull()

        override fun selectFollowUp(response: EvidenceResponse): Node? {
            val answer = (response as EvidenceResponseQuestionMultipleChoice).answerId
            if (!request.possibleValues.contains(answer)) {
                throw IllegalStateException("Invalid answer: $answer")
            }
            return followUpMap[answer]
        }
    }

    class IcaoNfcTunnelNode(
        override val nodeId: String,
        val dataGroups: List<Int>,
        private val successfulChipAuthentication: Node,
        private val successfulActiveAuthentication: Node,
        private val noAuthentication: Node): Node() {

        override val requests: List<EvidenceRequest>
            get() = listOf(EvidenceRequestIcaoNfcTunnel(
                EvidenceRequestIcaoNfcTunnelType.HANDSHAKE, 0, byteArrayOf()))
        override val followUps: Iterable<Node>
            get() = setOf(successfulActiveAuthentication, successfulChipAuthentication, noAuthentication)

        override fun selectFollowUp(response: EvidenceResponse): Node? {
            val resp = response as EvidenceResponseIcaoNfcTunnelResult
            return when (resp.advancedAuthenticationType) {
                EvidenceResponseIcaoNfcTunnelResult.AdvancedAuthenticationType.NONE -> noAuthentication
                EvidenceResponseIcaoNfcTunnelResult.AdvancedAuthenticationType.CHIP -> successfulChipAuthentication
                EvidenceResponseIcaoNfcTunnelResult.AdvancedAuthenticationType.ACTIVE -> successfulActiveAuthentication
            }
        }
    }
}