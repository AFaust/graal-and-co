package de.axelfaust.graal.truffle.instruments.api;

import java.util.List;
import java.util.Optional;

/**
 * Instances of this interface allow clients to inspect the composition of the current callstack in a Truffle-based polyglot runtime.
 *
 * The callstacks exposed by this service only reflect the current callstack within the guest language(s) being executed in the same thread
 * as the caller. Callstacks will not include any frames from the Java host environment and may contain mixed language frames if operations
 * / calls from guest different languages are nested.
 *
 * @author Axel Faust
 */
public interface PolyglotCallstackService
{

    /**
     * Retrieves the callstack of guest language(s) in the current thread. The callstack is ordered from current / most recent frame to
     * initial / oldest frame.
     *
     * @return the callstack (shallow-copy) - never {@code null}
     */
    List<PolyglotCallstackFrame> getCallstack();

    /**
     * Retrieves the current / most recent callstack frame of any guest language in the current thread.
     *
     * @return the current frame
     */
    Optional<PolyglotCallstackFrame> getCurrentFrame();

    /**
     * Retrieves the callstack frame responsible for the call into the {@link #getCurrentFrame() current frame}. This operation supports
     * ignoring any calls within the same source file as the current frame to focus on cross-source calls.
     *
     * @param ignoreSameSource
     *            {@code true} if calls from the same source as the current frame should be skipped
     * @return the frame (directly or indirectly depending on {@code ignoreSameSource}) responsible for a call to the code represented by
     *         the current frame
     */
    Optional<PolyglotCallstackFrame> getCallingFrame(boolean ignoreSameSource);
}
