package ch.obermuhlner.scriptengine.java;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import ch.obermuhlner.scriptengine.java.constructor.DefaultConstructorStrategy;
import ch.obermuhlner.scriptengine.java.execution.DefaultExecutionStrategy;
import ch.obermuhlner.scriptengine.java.execution.ExecutionStrategy;

/**
 * The compiled Java script created by a {@link JavaScriptEngine}.
 *
 * This class has been copied and modified from {@link ch.obermuhlner.scriptengine.java.JavaCompiledScript} to allow
 * having unused bindings or fields with incompatible types
 *
 * The class can't be moved to another package because the parent's class constructor is package-private
 */
public class JavaRuleCompiledScript extends JavaCompiledScript {
    private final JavaScriptEngine engine;
    private final Class<?> compiledClass;
    private final Object compiledInstance;
    private ExecutionStrategy executionStrategy;

    /**
     * Construct a {@link JavaRuleCompiledScript}.
     *
     * @param engine the {@link JavaScriptEngine} that compiled this script
     * @param compiledClass the compiled {@link Class}
     */
    public JavaRuleCompiledScript(JavaScriptEngine engine, Class<?> compiledClass) throws ScriptException {
        super(null, null, null, null);

        this.engine = engine;
        this.compiledClass = compiledClass;
        this.compiledInstance = DefaultConstructorStrategy.byDefaultConstructor().construct(compiledClass);
        this.executionStrategy = new DefaultExecutionStrategy(compiledClass);
    }

    /**
     * Returns the compiled {@link Class}.
     *
     * @return the compiled {@link Class}.
     */
    public Class<?> getCompiledClass() {
        return compiledClass;
    }

    /**
     * Returns the instance of the compiled {@link Class}.
     *
     * @return the instance of the compiled {@link Class} or {@code null}
     *         if no instance was created and only static methods will be called
     *         by the the {@link ExecutionStrategy}.
     */
    public Object getCompiledInstance() {
        return compiledInstance;
    }

    /**
     * Sets the {@link ExecutionStrategy} to be used when evaluating the compiled class instance.
     *
     * @param executionStrategy the {@link ExecutionStrategy}
     */
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

        pushVariables(globalBindings, engineBindings);
        Object result = executionStrategy.execute(compiledInstance);
        pullVariables(globalBindings, engineBindings);

        return result;
    }

    private void pushVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
        Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);

        for (Map.Entry<String, Object> entry : mergedBindings.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            try {
                Field field = compiledClass.getField(name);
                field.set(compiledInstance, value);
            } catch (IllegalAccessException e) {
                throw new ScriptException(e);
            } catch (NoSuchFieldException | IllegalArgumentException e) {
                // ignore fields that are not present or of wrong type
            }
        }
    }

    private void pullVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
        for (Field field : compiledClass.getFields()) {
            try {
                String name = field.getName();
                Object value = field.get(compiledInstance);
                setBindingsValue(globalBindings, engineBindings, name, value);
            } catch (IllegalAccessException e) {
                throw new ScriptException(e);
            }
        }
    }

    private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, String name, Object value) {
        if (engineBindings != null && engineBindings.containsKey(name)) {
            engineBindings.put(name, value);
        } else if (globalBindings != null && globalBindings.containsKey(name)) {
            globalBindings.put(name, value);
        }
    }

    private Map<String, Object> mergeBindings(Bindings... bindingsToMerge) {
        Map<String, Object> variables = new HashMap<>();

        for (Bindings bindings : bindingsToMerge) {
            if (bindings != null) {
                for (Map.Entry<String, Object> globalEntry : bindings.entrySet()) {
                    variables.put(globalEntry.getKey(), globalEntry.getValue());
                }
            }
        }

        return variables;
    }
}
