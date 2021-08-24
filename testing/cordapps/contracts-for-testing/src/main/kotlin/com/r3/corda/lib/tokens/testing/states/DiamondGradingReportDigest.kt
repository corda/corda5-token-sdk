package com.r3.corda.lib.tokens.testing.states

import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ClarityScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ColorScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.CutScale
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.UniqueIdentifier
import java.math.BigDecimal

/**
 * JsonRepresentable version of [DiamondGradingReport] for use in HTTP RPC calls.
 */
data class DiamondGradingReportDigest(
    val caratWeight: BigDecimal,
    val color: ColorScale,
    val clarity: ClarityScale,
    val cut: CutScale,
    val linearId: String
) : JsonRepresentable {

    constructor(
        diamondGradingReport: DiamondGradingReport
    ) : this(
        diamondGradingReport.caratWeight,
        diamondGradingReport.color,
        diamondGradingReport.clarity,
        diamondGradingReport.cut,
        diamondGradingReport.linearId
    )

    constructor(
        caratWeight: BigDecimal,
        color: ColorScale,
        clarity: ClarityScale,
        cut: CutScale,
        linearId: UniqueIdentifier
    ) : this(caratWeight, color, clarity, cut, linearId.toString())

    override fun toJsonString(): String {
        return """{ "caratWeight" : "$caratWeight", "color" : "$color", "clarity" : "$clarity", "cut" : "$cut", "linearId" : "$linearId"}""".trimIndent()
    }
}