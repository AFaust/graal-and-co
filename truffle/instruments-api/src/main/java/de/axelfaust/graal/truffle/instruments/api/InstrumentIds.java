package de.axelfaust.graal.truffle.instruments.api;

/**
 * Collection of IDs for instruments provided by this package.
 *
 * @author Axel Faust
 */
public interface InstrumentIds
{

    /**
     * ID of the instrument responsible for tracking the {@link PolyglotCallstackService polyglot callstack}
     */
    String POLYGLOT_CALLSTACK = "polyglot-callstack";
}
