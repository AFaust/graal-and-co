package de.axelfaust.graal.truffle.instruments.api;

import java.net.URI;

import com.oracle.truffle.api.source.SourceSection;

/**
 * Instances of this interface represent a single frame in the callstack of a Truffle-based guest language. Most operations in this
 * interface provide a more convenient access to information that may as well be obtained via the underlying {@link #getSourceSection()
 * source section}.
 *
 * @author Axel Faust
 */
public interface PolyglotCallstackFrame
{

    /**
     * Retrieves the full URI of the source file corresponding to this frame.
     *
     * @return the source URI
     */
    URI getSourceURI();

    /**
     * Retrieves the (simple) name of the resource containing the source. The return value of this operation will typically be shorter than
     * the full path / URI and correspond to a simple file name.
     *
     * @return the name of the source resource
     */
    String getSourceName();

    /**
     * Retrieves the ID of the language from which this frame originates.
     *
     * @return the ID of the language
     */
    String getLanguageId();

    /**
     * Retrieves the name of the language from which this frame originates.
     *
     * @return the name of the language
     */
    String getLanguageName();

    /**
     * Retrieves the name of the executable scope for this callstack frame. An executable scope may be either the root of a program /
     * script, or an executable component such as a function.
     *
     * @return the name of the scope or {@code null} if the scope has not been associated with a name - depending on the specific
     *         engine/language, this may also yield a pseudo-name for inherent scopes (such as {@code :program} for the root of a JavaScript
     *         script)
     */
    String getScopeName();

    /**
     * Retrieves the code fragment associated with this frame.
     *
     * @return the code fragment
     */
    CharSequence getCode();

    /**
     * Retrieves the line number within the source file that corresponds to this frame.
     *
     * @return the line number
     */
    int getLineNumber();

    /**
     * Retrieves the section of the source file corresponding to this frame.
     *
     * @return the source section for this frame
     */
    SourceSection getSourceSection();
}
