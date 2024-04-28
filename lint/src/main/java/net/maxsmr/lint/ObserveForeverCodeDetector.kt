package net.maxsmr.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class ObserveForeverCodeDetector: Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf(
            "observeForever"
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)
        val evaluator = context.evaluator
        if (evaluator.isMemberInClass(method, LIVE_DATA_CLASS_NAME)
                || evaluator.isMemberInSubClassOf(method, LIVE_DATA_CLASS_NAME)) {
            reportUsage(context, node)
        }
    }

    private fun reportUsage(context: JavaContext, node: UCallExpression) {
        context.report(
                issue = ISSUE,
                scope = node,
                location = context.getCallLocation(
                        call = node,
                        includeReceiver = true,
                        includeArguments = true
                ),
                message = DESCRIPTION
        )
    }

    companion object {

        private const val LIVE_DATA_CLASS_NAME = "androidx.lifecycle.LiveData"

        private const val DESCRIPTION = "Usage of \"observeForever\" is not recommended. Use [BaseOwnerViewModel.observe]"

        @JvmField
        val ISSUE: Issue = Issue.create(
                // ID: used in @SuppressLint warnings etc
                id = "ObserveForeverCodeDetector",
                // Title -- shown in the IDE's preference dialog, as category headers in the
                // Analysis results window, etc
                briefDescription = DESCRIPTION,
                // Full explanation of the issue; you can use some markdown markup such as
                // `monospace`, *italic*, and **bold**.
                explanation = """
                    Due to memory leaks should be replaced by [BaseOwnerViewModel.observe]
                    """, // no need to .trimIndent(), lint does that automatically
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.WARNING,
                implementation = Implementation(
                        ObserveForeverCodeDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                )
        )
    }
}