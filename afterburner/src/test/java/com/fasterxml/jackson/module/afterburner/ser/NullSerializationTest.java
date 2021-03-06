package com.fasterxml.jackson.module.afterburner.ser;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import com.fasterxml.jackson.module.afterburner.AfterburnerTestBase;

// Copied from [com.fasterxml.jackson.databind.ser.filter]
public class NullSerializationTest extends AfterburnerTestBase
{
    static class NullSerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
            throws IOException
        {
            gen.writeString("foobar");
        }
    }

    static class Bean1 {
        public String name = null;
    }

    static class Bean2 {
        public String type = null;
    }
    
    @SuppressWarnings("serial")
    static class MyNullProvider extends DefaultSerializerProvider
    {
        public MyNullProvider() { super(new JsonFactory()); }
        public MyNullProvider(MyNullProvider base, SerializationConfig config, 
                GeneratorSettings genSettings,
                SerializerFactory jsf) {
            super(base, config, genSettings, jsf);
        }

        @Override
        public DefaultSerializerProvider createInstance(SerializationConfig config,
                GeneratorSettings genSettings, SerializerFactory jsf) {
            return new MyNullProvider(this, config, genSettings, jsf);
        }

        @Override
        public JsonSerializer<Object> findNullValueSerializer(BeanProperty property)
            throws JsonMappingException
        {
            if ("name".equals(property.getName())) {
                return new NullSerializer();
            }
            return super.findNullValueSerializer(property);
        }
    }

    static class BeanWithNullProps
    {
        @JsonSerialize(nullsUsing=NullSerializer.class)
        public String a = null;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testSimple() throws Exception
    {
        assertEquals("null", MAPPER.writeValueAsString(null));
    }

    public void testOverriddenDefaultNulls() throws Exception
    {
        DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl(new JsonFactory());
        sp.setNullValueSerializer(new NullSerializer());
        ObjectMapper m = ObjectMapper.builder()
                .serializerProvider(sp)
                .build();
        assertEquals("\"foobar\"", m.writeValueAsString(null));
    }

    public void testCustomPOJONullsViaProvider() throws Exception
    {
        ObjectMapper m = ObjectMapper.builder()
                .serializerProvider(new MyNullProvider())
                .build();
        assertEquals("{\"name\":\"foobar\"}", m.writeValueAsString(new Bean1()));
        assertEquals("{\"type\":null}", m.writeValueAsString(new Bean2()));
    }

    public void testCustomTreeNullsViaProvider() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putNull("a");

        // by default, null is... well, null
        assertEquals("{\"a\":null}", MAPPER.writeValueAsString(root));

        // but then we can customize it:
        DefaultSerializerProvider prov = new MyNullProvider();
        prov.setNullValueSerializer(new NullSerializer());
        ObjectMapper m = ObjectMapper.builder()
                .serializerProvider(prov)
                .build();
        assertEquals("{\"a\":\"foobar\"}", m.writeValueAsString(root));
    }

    public void testNullSerializeViaPropertyAnnotation() throws Exception
    {
        assertEquals("{\"a\":\"foobar\"}", MAPPER.writeValueAsString(new BeanWithNullProps()));
    }
}
