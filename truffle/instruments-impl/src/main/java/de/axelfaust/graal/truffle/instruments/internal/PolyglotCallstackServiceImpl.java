package de.axelfaust.graal.truffle.instruments.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackFrame;
import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackService;

/**
 *
 * @author Axel Faust
 */
class PolyglotCallstackServiceImpl implements PolyglotCallstackService
{

    // TODO Need a different logging approach since SLF4J won't be accessible
    // private static final Logger LOGGER = LoggerFactory.getLogger(PolyglotCallstackServiceImpl.class);

    /**
     *
     * @author Axel Faust
     */
    protected static class TrackedFrame
    {

        protected final PolyglotCallstackFrame frame;

        protected int contextEnterCount = 0;

        protected TrackedFrame(final PolyglotCallstackFrame frame)
        {
            this.frame = frame;
        }

        protected PolyglotCallstackFrame getFrame()
        {
            return this.frame;
        }

        protected boolean isSameFrame(final PolyglotCallstackFrame scriptContext)
        {
            return this.frame.equals(scriptContext);
        }

        protected void recordEnter()
        {
            this.contextEnterCount++;
        }

        protected int recordLeave()
        {
            return --this.contextEnterCount;
        }
    }

    protected final ThreadLocal<List<TrackedFrame>> callstack = new ThreadLocal<List<TrackedFrame>>()
    {

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected List<TrackedFrame> initialValue()
        {
            return new LinkedList<>();
        }
    };

    protected final ThreadLocal<TrackedFrame> currentFrame = new ThreadLocal<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<PolyglotCallstackFrame> getCallstack()
    {
        final TrackedFrame currentTrackedFrame = this.currentFrame.get();
        List<PolyglotCallstackFrame> callstack;

        if (currentTrackedFrame != null)
        {
            final List<TrackedFrame> currentCallstack = this.callstack.get();
            callstack = new ArrayList<>(currentCallstack.size() + 1);
            callstack.add(currentTrackedFrame.getFrame());
            currentCallstack.forEach(trackedFrame -> callstack.add(trackedFrame.getFrame()));
        }
        else
        {
            callstack = Collections.emptyList();
        }
        return callstack;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Optional<PolyglotCallstackFrame> getCurrentFrame()
    {
        final TrackedFrame currentTrackedFrame = this.currentFrame.get();
        final Optional<PolyglotCallstackFrame> currentFrame = currentTrackedFrame != null ? Optional.of(currentTrackedFrame.getFrame())
                : Optional.empty();
        return currentFrame;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Optional<PolyglotCallstackFrame> getCallingFrame(final boolean ignoreSameSource)
    {
        Optional<PolyglotCallstackFrame> callingFrame = Optional.empty();

        final TrackedFrame currentTrackedFrame = this.currentFrame.get();
        if (currentTrackedFrame != null)
        {
            final List<TrackedFrame> currentCallstack = this.callstack.get();
            final URI sourceURI = currentTrackedFrame.getFrame().getSourceURI();

            callingFrame = currentCallstack.stream().map(TrackedFrame::getFrame)
                    .filter(frame -> !ignoreSameSource || (sourceURI != null && !sourceURI.equals(frame.getSourceURI()))).findFirst();
        }

        return callingFrame;
    }

    protected void recordEnter(final PolyglotCallstackFrame frame)
    {
        // LOGGER.debug("recordEnter called for {}", frame);
        TrackedFrame currentFrame = this.currentFrame.get();
        if (currentFrame == null)
        {
            // LOGGER.debug("Setting {} as initial frame", frame);
            currentFrame = new TrackedFrame(frame);
            this.currentFrame.set(currentFrame);
        }
        else if (!currentFrame.isSameFrame(frame))
        {
            // LOGGER.debug("Pushing {} to stack", currentFrame.getFrame());
            this.callstack.get().add(0, currentFrame);
            currentFrame = new TrackedFrame(frame);
            this.currentFrame.set(currentFrame);
        }
        else
        {
            // LOGGER.debug("Current frame {} is being re-entered", frame);
        }
        currentFrame.recordEnter();
    }

    protected void recordLeave(final PolyglotCallstackFrame frame)
    {
        // LOGGER.debug("recordLeave called for {}", frame);
        final TrackedFrame currentFrame = this.currentFrame.get();
        if (currentFrame == null || !currentFrame.isSameFrame(frame))
        {
            // LOGGER.warn("recordLeave called for {} without a previous, matching recordEnter", frame);
        }
        else
        {
            final int enterCallsLeftToCompensate = currentFrame.recordLeave();
            if (enterCallsLeftToCompensate == 0)
            {
                final List<TrackedFrame> currentCallstack = this.callstack.get();
                if (!currentCallstack.isEmpty())
                {
                    this.currentFrame.set(currentCallstack.remove(0));
                    // LOGGER.debug("Unwound {} from stack", currentFrame.getFrame());
                }
                else
                {
                    this.currentFrame.remove();
                    // LOGGER.debug("Unwound last frame");
                }
            }
            else
            {
                // LOGGER.debug("{} calls left to unwind for {}", enterCallsLeftToCompensate, currentFrame.getFrame());
            }
        }
    }
}
