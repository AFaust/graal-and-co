package de.axelfaust.graal.truffle.instruments;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import de.axelfaust.graal.truffle.instruments.api.InstrumentIds;
import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackFrame;
import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackService;

/**
 *
 * @author Axel Faust
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class CallstackServiceTests
{

    @Test
    public void emptyStackWithoutGuestLanguageInvocation()
    {
        final Context context = Context.newBuilder().allowHostAccess(true).build();
        try
        {
            final Engine engine = context.getEngine();
            final Map<String, Instrument> instruments = engine.getInstruments();
            final PolyglotCallstackService callstackService = instruments.get(InstrumentIds.POLYGLOT_CALLSTACK)
                    .lookup(PolyglotCallstackService.class);

            callstackService.getCurrentFrame().ifPresent(frame -> {
                System.out.println(frame); // info on failure
                Assert.fail("There should be no current frame without execution of a guest language source");
            });
            callstackService.getCallingFrame(false).ifPresent(frame -> {
                System.out.println(frame); // info on failure
                Assert.fail("There should be no calling frame without execution of a guest language source");
            });
            callstackService.getCallingFrame(true).ifPresent(frame -> {
                System.out.println(frame); // info on failure
                Assert.fail("There should be no calling frame without execution of a guest language source");
            });
            final List<PolyglotCallstackFrame> callstack = callstackService.getCallstack();
            Assert.assertTrue("The callstack should be empty without execution of a guest language", callstack.isEmpty());
        }
        finally
        {
            context.close();
        }
    }

    @Test
    public void stackWithCallInSimpleJavaScriptProgram() throws Exception
    {
        final Context context = Context.newBuilder().allowHostAccess(true).build();
        try
        {
            final Engine engine = context.getEngine();
            final Map<String, Instrument> instruments = engine.getInstruments();
            final PolyglotCallstackService callstackService = instruments.get(InstrumentIds.POLYGLOT_CALLSTACK)
                    .lookup(PolyglotCallstackService.class);

            final Value jsBindings = context.getBindings("js");
            final AtomicBoolean checkCalled = new AtomicBoolean(false);
            final Supplier<Void> callstackCheck = () -> {
                checkCalled.compareAndSet(false, true);

                final Optional<PolyglotCallstackFrame> currentFrame = callstackService.getCurrentFrame();
                Assert.assertTrue("There should be a currentFrame", currentFrame.isPresent());
                currentFrame.ifPresent(frame -> {
                    Assert.assertNotNull("A generated source URI should have been available", frame.getSourceURI());
                    Assert.assertEquals("checkProgram.js", frame.getSourceName());
                    Assert.assertEquals("js", frame.getLanguageId());
                    Assert.assertEquals("JavaScript", frame.getLanguageName());
                    Assert.assertEquals(":program", frame.getScopeName());
                    Assert.assertEquals(1, frame.getLineNumber());
                    Assert.assertNotNull("A source section should be attached to the frame", frame.getSourceSection());

                    // TODO Reenable once we know how to expose polyglot source section instead of Truffle object
                    // Assert.assertEquals(frame.getSourceName(), frame.getSourceSection().getSource().getName());
                    // Assert.assertEquals(frame.getSourceURI(), frame.getSourceSection().getSource().getURI());
                    // Assert.assertEquals(frame.getLineNumber(), frame.getSourceSection().getStartLine());
                });

                final Optional<PolyglotCallstackFrame> callingFrame = callstackService.getCallingFrame(false);
                Assert.assertFalse("There should be no callingFrame", callingFrame.isPresent());

                final List<PolyglotCallstackFrame> callstack = callstackService.getCallstack();
                Assert.assertEquals(1, callstack.size());
                Assert.assertEquals(currentFrame.get(), callstack.get(0));

                return null;
            };
            final Function<String, Void> callstackCheckWithArgument = arg -> callstackCheck.get();
            jsBindings.putMember("check", callstackCheck);
            jsBindings.putMember("checkWithArg", callstackCheckWithArgument);

            final Source source = Source.newBuilder("js", "check(); checkWithArg('Test');", "checkProgram.js").build();
            context.eval(source);

            Assert.assertTrue("check() should have been called", checkCalled.get());
        }
        finally
        {
            context.close();
        }
    }

    @Test
    public void emptyStackWithAccessToMemberInSimpleJavaScriptProgram() throws Exception
    {
        final Context context = Context.newBuilder().allowHostAccess(true).build();
        try
        {
            final Engine engine = context.getEngine();
            final Map<String, Instrument> instruments = engine.getInstruments();
            final PolyglotCallstackService callstackService = instruments.get(InstrumentIds.POLYGLOT_CALLSTACK)
                    .lookup(PolyglotCallstackService.class);

            final Value jsBindings = context.getBindings("js");
            final AtomicInteger proxyMemberAccessed = new AtomicInteger(0);
            final ProxyObject proxyObject = new ProxyObject()
            {

                @Override
                public boolean hasMember(final String key)
                {
                    return "member".equals(key);
                }

                @Override
                public Object getMemberKeys()
                {
                    return new String[] { "member" };
                }

                @Override
                public void putMember(final String key, final Value value)
                {
                    if ("member".equals(key))
                    {
                        proxyMemberAccessed.incrementAndGet();

                        callstackService.getCurrentFrame().ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no current frame for access to a member without any prior call");
                        });
                        callstackService.getCallingFrame(false).ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no calling frame for access to a member without any prior call");
                        });
                        callstackService.getCallingFrame(true).ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no calling frame for access to a member without any prior call");
                        });
                        final List<PolyglotCallstackFrame> callstack = callstackService.getCallstack();
                        Assert.assertTrue("The callstack should be empty for access to a member without any prior call",
                                callstack.isEmpty());
                    }
                }

                @Override
                public Object getMember(final String key)
                {
                    if ("member".equals(key))
                    {
                        proxyMemberAccessed.incrementAndGet();

                        callstackService.getCurrentFrame().ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no current frame for access to a member without any prior call");
                        });
                        callstackService.getCallingFrame(false).ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no calling frame for access to a member without any prior call");
                        });
                        callstackService.getCallingFrame(true).ifPresent(frame -> {
                            System.out.println(frame); // info on failure
                            Assert.fail("There should be no calling frame for access to a member without any prior call");
                        });
                        final List<PolyglotCallstackFrame> callstack = callstackService.getCallstack();
                        Assert.assertTrue("The callstack should be empty for access to a member without any prior call",
                                callstack.isEmpty());
                    }

                    return "dummy";
                }
            };
            jsBindings.putMember("proxy", proxyObject);

            final Source source = Source.newBuilder("js",
                    "function fn(v, b){if(b){print(v);}} proxy.member = 'Test'; fn(proxy.member, false);", "checkProgram.js").build();
            context.eval(source);

            Assert.assertEquals("proxy.member should have been accessed two times", 2, proxyMemberAccessed.get());
        }
        finally
        {
            context.close();
        }
    }

}
