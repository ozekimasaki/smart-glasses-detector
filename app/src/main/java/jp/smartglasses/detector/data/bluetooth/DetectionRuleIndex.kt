package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.util.DetectionRule

internal class DetectionRuleIndex(
    private val rules: List<DetectionRule>
) {
    private data class IndexedRule(
        val order: Int,
        val rule: DetectionRule
    )

    private val indexed = rules.mapIndexed { order, rule ->
        IndexedRule(order = order, rule = rule)
    }

    private val companyIdOnlyByCompanyId = HashMap<Int, MutableList<IndexedRule>>()
    private val byCompanyId = HashMap<Int, MutableList<IndexedRule>>()
    private val byNormalizedUuid = HashMap<String, MutableList<IndexedRule>>()
    private val payloadRuleList = ArrayList<DetectionRule>()
    private val suffixRuleList = ArrayList<DetectionRule>()
    private val nameRuleList = ArrayList<DetectionRule>()

    init {
        for (indexedRule in indexed) {
            val rule = indexedRule.rule
            for (companyId in rule.companyIds) {
                byCompanyId.getOrPut(companyId) { ArrayList() }.add(indexedRule)
                if (rule.allowCompanyIdOnly) {
                    companyIdOnlyByCompanyId.getOrPut(companyId) { ArrayList() }.add(indexedRule)
                }
            }
            for (uuid in rule.serviceUuids) {
                byNormalizedUuid.getOrPut(BleUuid.normalize(uuid)) { ArrayList() }.add(indexedRule)
            }
            if (rule.payloadPatterns.isNotEmpty()) {
                payloadRuleList += rule
            }
            if (rule.manufacturerDataSuffixes.isNotEmpty()) {
                suffixRuleList += rule
            }
            if (rule.namePatterns.isNotEmpty() || rule.nameRegexes.isNotEmpty()) {
                nameRuleList += rule
            }
        }
    }

    fun companyIdOnlyRules(companyIds: Set<Int>): List<DetectionRule> {
        return select(companyIds) { companyId -> companyIdOnlyByCompanyId[companyId] }
    }

    fun companyIdRules(companyIds: Set<Int>): List<DetectionRule> {
        return select(companyIds) { companyId -> byCompanyId[companyId] }
    }

    fun uuidRules(normalizedUuids: Collection<String>): List<DetectionRule> {
        return select(normalizedUuids) { uuid -> byNormalizedUuid[uuid] }
    }

    fun payloadRules(): List<DetectionRule> = payloadRuleList

    fun suffixRules(): List<DetectionRule> = suffixRuleList

    fun nameRules(): List<DetectionRule> = nameRuleList

    private fun <T> select(
        keys: Collection<T>,
        lookup: (T) -> List<IndexedRule>?
    ): List<DetectionRule> {
        if (keys.isEmpty()) {
            return emptyList()
        }

        val seen = BooleanArray(indexed.size)
        val selected = ArrayList<IndexedRule>()
        for (key in keys) {
            val matches = lookup(key) ?: continue
            for (indexedRule in matches) {
                if (!seen[indexedRule.order]) {
                    seen[indexedRule.order] = true
                    selected += indexedRule
                }
            }
        }
        if (selected.size > 1) {
            selected.sortBy { indexedRule -> indexedRule.order }
        }
        return selected.map { indexedRule -> indexedRule.rule }
    }
}
