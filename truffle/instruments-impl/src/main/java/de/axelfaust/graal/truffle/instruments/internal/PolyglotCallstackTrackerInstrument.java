package de.axelfaust.graal.truffle.instruments.internal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;

import de.axelfaust.graal.truffle.instruments.api.InstrumentIds;
import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackFrame;
import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackService;

/**
 *
 * @author Axel Faust
 */
@Registration(id = InstrumentIds.POLYGLOT_CALLSTACK, services = { PolyglotCallstackService.class })
public class PolyglotCallstackTrackerInstrument extends TruffleInstrument
{

    // TODO Need a different logging approach since SLF4J won't be accessible
    // private static final Logger LOGGER = LoggerFactory.getLogger(PolyglotCallstackTrackerInstrument.class);

    /**
     *
     * @author Axel Faust
     */
    protected static class CallNode extends ExecutionEventNode
    {

        private final PolyglotCallstackServiceImpl service;

        private final PolyglotCallstackFrame frame;

        @CompilationFinal
        private int inputCount = -1;

        protected CallNode(final PolyglotCallstackServiceImpl service, final PolyglotCallstackFrame frame)
        {
            this.service = service;
            this.frame = frame;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected void onEnter(final VirtualFrame frame)
        {
            if (this.inputCount == -1)
            {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.inputCount = this.getInputCount();
            }

            if (this.inputCount == 0)
            {
                this.service.recordEnter(this.frame);
            }
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected void onInputValue(final VirtualFrame frame, final EventContext inputContext, final int inputIndex,
                final Object inputValue)
        {
            if (this.inputCount != 0)
            {
                if (this.inputCount == -1)
                {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.inputCount = this.getInputCount();
                }

                // don't care about the actual values - just need to call recordEnter after last input node has been resolved
                if (this.inputCount - 1 == inputIndex)
                {
                    this.service.recordEnter(this.frame);
                }
            }
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected void onReturnValue(final VirtualFrame frame, final Object result)
        {
            this.service.recordLeave(this.frame);
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected void onReturnExceptional(final VirtualFrame frame, final Throwable exception)
        {
            this.service.recordLeave(this.frame);
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected Object onUnwind(final VirtualFrame frame, final Object info)
        {
            this.service.recordLeave(this.frame);
            return null;
        }
    }

    /**
     *
     * @author Axel Faust
     */
    protected static class CallNodeFactory implements ExecutionEventNodeFactory
    {

        private final PolyglotCallstackServiceImpl service;

        protected CallNodeFactory(final PolyglotCallstackServiceImpl service)
        {
            this.service = service;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public ExecutionEventNode create(final EventContext ctxt)
        {
            return new CallNode(this.service, new PolyglotCallstackFrameImpl(ctxt));
        }
    }

    /**
     *
     * @param env
     */
    @Override
    protected void onCreate(final Env env)
    {
        // LOGGER.debug("Setting up instrument {}", InstrumentIds.POLYGLOT_CALLSTACK);

        final PolyglotCallstackServiceImpl service = new PolyglotCallstackServiceImpl();
        env.registerService(service);

        final SourceSectionFilter callFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.CallTag.class).build();
        final SourceSectionFilter expressionFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.ExpressionTag.class).build();

        // LOGGER.debug("Completed setup of instrument {}", InstrumentIds.POLYGLOT_CALLSTACK);

        env.getInstrumenter().attachExecutionEventFactory(callFilter, expressionFilter, new CallNodeFactory(service));
    }
}
