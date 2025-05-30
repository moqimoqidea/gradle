/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.compile;

import com.google.common.annotations.VisibleForTesting;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.api.DiagnosticFormatter;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Log;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.problems.Problem;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.ProblemSpec;
import org.gradle.api.problems.Problems;
import org.gradle.api.problems.Severity;
import org.gradle.api.problems.internal.GradleCoreProblemGroup;
import org.gradle.api.problems.internal.InternalProblemReporter;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.tools.Diagnostic.NOPOS;

/**
 * A {@link DiagnosticListener} that consumes {@link Diagnostic} messages, and reports them as Gradle {@link Problems}.
 */
// If this annotation is not present, all diagnostic messages would be wrapped in a ClientCodeWrapper.
// We don't need this wrapping feature, hence the trusted annotation.
@ClientCodeWrapper.Trusted
public class DiagnosticToProblemListener implements DiagnosticListener<JavaFileObject> {
    public static final String FORMATTER_FALLBACK_MESSAGE = "Failed to format diagnostic message, falling back to default message formatting";
    private static final Logger LOGGER = Logging.getLogger(DiagnosticToProblemListener.class);

    private final Context context;
    private final InternalProblemReporter problemReporter;
    private final List<Problem> problemsReported = new ArrayList<>();

    private int errorCount = 0;
    private int warningCount = 0;

    public DiagnosticToProblemListener(InternalProblemReporter problemReporter, Context context) {
        this.problemReporter = problemReporter;
        this.context = context;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        switch (diagnostic.getKind()) {
            case ERROR:
                errorCount++;
                break;
            case WARNING:
            case MANDATORY_WARNING:
                warningCount++;
                break;
            default:
                break;
        }

        Problem reportedProblem = problemReporter.create(id(diagnostic), spec -> buildProblem(diagnostic, spec));
        problemsReported.add(reportedProblem);
    }

    private static ProblemId id(Diagnostic<? extends JavaFileObject> diagnostic) {
        String code = diagnostic.getCode();
        String message = diagnostic.getMessage(Locale.getDefault());
        return ProblemId.create(
            code == null ? "unknown" : code,
            message == null ? "unknown" : message,
            GradleCoreProblemGroup.compilation().java()
        );
    }

    /**
     * This method is responsible for printing the number of errors and warnings after writing the diagnostics out to the console.
     * This count is normally printed by the compiler itself, but when a {@link DiagnosticListener} is registered, the compiled will stop reporting the number of errors and warnings.
     *
     * An example output with the last two lines being the count:
     * <pre>
     * /.../src/main/java/Foo.java:10: error: ';' expected
     *                     String s = "Hello, World!"
     *                                               ^
     * /.../Bar.java:10: warning: [cast] redundant cast to String
     *                     String s = (String)"Hello World";
     *                                ^
     * 1 error
     * 1 warning
     * </pre>
     *
     * @see com.sun.tools.javac.main.JavaCompiler#printCount(String, int)
     */
    String diagnosticCounts() {
        Log logger = Log.instance(new Context());
        Optional<String> error = diagnosticCount(logger, "error", errorCount);
        Optional<String> warning = diagnosticCount(logger, "warn", warningCount);

        return Stream.of(error, warning)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Formats and prints the number of diagnostics of a given kind.
     * <p>
     * E.g.:
     * <pre>
     * 1 error
     * 2 warnings
     * </pre>
     *
     * @param logger the logger used to localize the message
     * @param kind the kind of diagnostic (error, or warn)
     * @param number the total number of diagnostics of the given kind
     * @return the human-readable count of diagnostics of the given kind, or {@code #Optional.empty()} if there are no diagnostics of the given kind
     */
    private static Optional<String> diagnosticCount(Log logger, String kind, int number) {
        // Compiler only handles 'error' and 'warn' kinds
        if (!("error".equals(kind) || "warn".equals(kind))) {
            throw new IllegalArgumentException("kind must be either 'error' or 'warn'");
        }
        // If there are no diagnostics of this kind, we don't need to print anything
        if (number == 0) {
            return Optional.empty();
        }

        // See the distributions' respective `compiler.java` files to see the keys used for localization.
        // We are using the following keys:
        //  - count.error and count.error.plural
        //  - count.warn and count.warn.plural
        StringBuilder keyBuilder = new StringBuilder("count.");
        keyBuilder.append(kind);
        if (number > 1) {
            keyBuilder.append(".plural");
        }

        return Optional.of(logger.localize(keyBuilder.toString(), number));
    }

    @VisibleForTesting
    void buildProblem(Diagnostic<? extends JavaFileObject> diagnostic, ProblemSpec spec) {
        addSeverity(diagnostic, spec);
        addLocations(diagnostic, spec);

        String label = toFormattedLabel(diagnostic);
        addContextualLabel(label, spec);

        String details = toFormattedDetails(diagnostic);
        // We cannot be sure that the compiler makes us a message, hence the defensiveness
        if (details != null) {
            addDetails(details, spec);
            // NOTE: This is required to keep backward compatibility
            // By default, when a compiler is called without a diagnostic listener
            // the compiler will print the diagnostic message to the error stream
            System.err.println(details);
        }
    }

    /**
     * Adds a contextual label to the problem spec.
     * <p>
     * This method will sanitize the label by splitting it into lines, and only using the first line.
     *
     * @param label the label to add
     * @param spec the problem spec to add the label to
     */
    private static void addContextualLabel(String label, ProblemSpec spec) {
        String[] lines = label.split(System.lineSeparator(), 2);
        spec.contextualLabel(lines[0]);
    }

    private static void addDetails(String formattedMessage, ProblemSpec spec) {
        spec.details(formattedMessage);
    }

    private static void addSeverity(Diagnostic<? extends JavaFileObject> diagnostic, ProblemSpec spec) {
        Severity severity = mapKindToSeverity(diagnostic.getKind());
        spec.severity(severity);
        if (severity == Severity.ERROR) {
            spec.solution(CompilationFailedException.RESOLUTION_MESSAGE);
        }
    }

    private static void addLocations(Diagnostic<? extends JavaFileObject> diagnostic, ProblemSpec spec) {
        String resourceName = diagnostic.getSource() != null ? getPath(diagnostic.getSource()) : null;
        int line = clampLocation(diagnostic.getLineNumber());
        int column = clampLocation(diagnostic.getColumnNumber());
        int position = clampLocation(diagnostic.getPosition());
        int end = clampLocation(diagnostic.getEndPosition());

        // We only set the location if we have a resource to point to
        if (resourceName != null) {
            // If we know the line ...
            if (NOPOS != line) {
                // ... and the column ...
                if (NOPOS != column) {
                    // ... and we know how long the error is (i.e. end - start)
                    // (documentation says that getEndPosition() will be NOPOS if and only if the getPosition() is NOPOS)
                    if (NOPOS != position) {
                        // ... we can report the line, column, and extent ...
                        spec.lineInFileLocation(resourceName, line, column, end - position);
                    } else {
                        // ... otherwise we can still report the line and column
                        spec.lineInFileLocation(resourceName, line, column);
                    }
                } else {
                    // ... otherwise we can still report the line
                    spec.lineInFileLocation(resourceName, line);
                }
            } else
                // If we know the offsets ...
                // (offset doesn't require line and column to be set, hence the separate check)
                // (documentation says that getEndPosition() will be NOPOS iff getPosition() is NOPOS)
                if (NOPOS != position && end > position) {
                    // ... we can report the start and extent
                    spec.offsetInFileLocation(resourceName, position, end - position);
                } else {
                    spec.fileLocation(resourceName);
                }

        }
    }

    /**
     * Using a {@link DiagnosticFormatter}, turns a diagnostic into a human-readable multi-line message.
     * <p>
     * This method uses an internal Java compiler API to get a formatter.
     * <p>
     * In some circumstances, getting the formatter can fail, after which we would use a
     * fail-safe way of formatting the message.
     * The drawback is that the formatters are not equal. Normally, we would get a {@code RichDiagnosticFormatter}
     * instance that can simplify types, generics, and is the formatter normally used by {@code javac}.
     * <p>
     * The failsafe (normally {@code BasicDiagnosticFormatter}, however, uses a much simpler algorithm to format the message,
     * and will make a different, more terse message.
     **/
    private String toFormattedDetails(Diagnostic<? extends JavaFileObject> diagnostic) {
        try {
            DiagnosticFormatter<JCDiagnostic> formatter = Log.instance(context).getDiagnosticFormatter();
            // Note: this method uses a different formatter than #toFormattedLabel
            return formatter.format((JCDiagnostic) diagnostic, JavacMessages.instance(context).getCurrentLocale());
        } catch (Exception ex) {
            LOGGER.info(FORMATTER_FALLBACK_MESSAGE);
            return diagnostic.getMessage(Locale.getDefault());
        }
    }

    /**
     * Using a {@link DiagnosticFormatter}, turns a diagnostic into a human-readable label.
     * <p>
     * This method uses an internal Java compiler API to get a formatter.
     * <p>
     * In some circumstances, getting the formatter can fail, after which we would use a
     * fail-safe way of formatting the message.
     * The drawback is that the formatters are not equal. Normally, we would get a {@code RichDiagnosticFormatter}
     * instance that can simplify types, generics, and is the formatter normally used by {@code javac}.
     * <p>
     * The failsafe (normally {@code BasicDiagnosticFormatter}, however, uses a much simpler algorithm to format the message,
     * and will make a different, more terse message.
     **/
    private String toFormattedLabel(Diagnostic<? extends JavaFileObject> diagnostic) {
        try {
            DiagnosticFormatter<JCDiagnostic> formatter = Log.instance(context).getDiagnosticFormatter();
            // Note: this method uses a different formatter than #toFormattedDetails
            return formatter.formatMessage((JCDiagnostic) diagnostic, JavacMessages.instance(context).getCurrentLocale());
        } catch (Exception ex) {
            LOGGER.info(FORMATTER_FALLBACK_MESSAGE);
            return diagnostic.getMessage(Locale.getDefault());
        }
    }

    /**
     * Clamp the value to an int, or return {@link Diagnostic#NOPOS} if the value is too large.
     * <p>
     * This is used to ensure that we don't report invalid locations.
     *
     * @param value the value to clamp
     * @return either the clamped value, or {@link Diagnostic#NOPOS}
     */
    private static int clampLocation(long value) {
        if (value > Integer.MAX_VALUE) {
            return Math.toIntExact(NOPOS);
        } else {
            return (int) value;
        }
    }

    private static String getPath(JavaFileObject fileObject) {
        return fileObject.getName();
    }

    private static Severity mapKindToSeverity(Diagnostic.Kind kind) {
        switch (kind) {
            case ERROR:
                return Severity.ERROR;
            case WARNING:
            case MANDATORY_WARNING:
                return Severity.WARNING;
            case NOTE:
            case OTHER:
            default:
                return Severity.ADVICE;
        }
    }

    public List<Problem> getReportedProblems() {
        return Collections.unmodifiableList(problemsReported);
    }
}
