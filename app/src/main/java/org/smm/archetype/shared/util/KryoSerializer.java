package org.smm.archetype.shared.util;

import com.alibaba.fastjson2.TypeReference;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigDecimalSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ClassSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.TimeSerializers.DurationSerializer;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 高性能序列化工具类
 *
 * 基于Kryo实现的高性能序列化工具，支持Java原生类型、时间类型、集合类型等多种数据类型的序列化。
 * 内置对常见Java类的自定义序列化器，如Instant、LocalDate、LocalTime、LocalDateTime等时间类型，
 * 以及Optional、Currency、Locale、InetAddress、URL、Class、BigDecimal、Pattern等特殊类型。
 *
 * 特性：
 * 1. 使用ThreadLocal保证线程安全
 * 2. 支持循环引用处理
 * 3. 支持泛型类型反序列化
 * 4. 内置类型缓存提升性能
 */
public final class KryoSerializer {

    // 序列化器
    private static final Map<Class<?>, Serializer<?>> DEFAULT_SERIALIZERS = new HashMap<>();

    // 使用ThreadLocal来替代KryoPool，提供线程安全的对象复用
    private static final ThreadLocal<Kryo> KYRO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);   // 关闭强制注册
        kryo.setReferences(true);              // 启用循环引用支持
        kryo.setAutoReset(true);               // 自动重置避免状态污染
        DEFAULT_SERIALIZERS.forEach(kryo::register);
        // 对 InetAddress 和 URL 的子类也使用自定义序列化器，避免 Kryo 对子类使用反射序列化器
        kryo.addDefaultSerializer(InetAddress.class, DEFAULT_SERIALIZERS.get(InetAddress.class));
        kryo.addDefaultSerializer(URL.class, DEFAULT_SERIALIZERS.get(URL.class));
        return kryo;
    });

    // 类型缓存 (提高性能)
    private static final Map<Type, Class<?>> TYPE_TO_CLASS_CACHE = new HashMap<>();

    static {
        // Java Time
        DEFAULT_SERIALIZERS.put(Instant.class, new InstantSerializer());
        DEFAULT_SERIALIZERS.put(LocalDate.class, new LocalDateSerializer());
        DEFAULT_SERIALIZERS.put(LocalTime.class, new LocalTimeSerializer());
        DEFAULT_SERIALIZERS.put(LocalDateTime.class, new LocalDateTimeSerializer());
        DEFAULT_SERIALIZERS.put(ZonedDateTime.class, new ZonedDateTimeSerializer());
        DEFAULT_SERIALIZERS.put(Duration.class, new DurationSerializer());
        DEFAULT_SERIALIZERS.put(Serializable.class, new JavaSerializer());
        // 其他常用类
        DEFAULT_SERIALIZERS.put(Optional.class, new OptionalSerializer());
        DEFAULT_SERIALIZERS.put(Currency.class, new CurrencySerializer());
        DEFAULT_SERIALIZERS.put(Locale.class, new LocaleSerializer());
        DEFAULT_SERIALIZERS.put(InetAddress.class, new InetAddressSerializer());
        DEFAULT_SERIALIZERS.put(URL.class, new URLSerializer());
        DEFAULT_SERIALIZERS.put(Class.class, new ClassSerializer());
        DEFAULT_SERIALIZERS.put(BigDecimal.class, new BigDecimalSerializer());
        DEFAULT_SERIALIZERS.put(Pattern.class, new PatternSerializer());
    }

    // 私有构造器
    private KryoSerializer() {}

    /**
     * 将对象序列化为字节数组
     *
     * 使用Kryo序列化器将传入的对象序列化为字节数组。该方法不接受null对象，
     * 并会在序列化完成后自动重置Kryo实例以避免状态污染。
     * @param obj 待序列化的对象，不能为null
     * @return 序列化后的字节数组
     * @throws IllegalArgumentException 当传入对象为null时抛出
     */
    public static byte[] serialize(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot serialize null object");
        }

        Kryo kryo = KYRO_THREAD_LOCAL.get();
        try (Output output = new Output(1024, -1)) {
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        } finally {
            // 清理可能存在的循环引用跟踪等状态
            kryo.reset();
        }
    }

    /**
     * 将字节数组反序列化为指定类型的对象（使用TypeReference支持复杂泛型）
     *
     * 使用Kryo反序列化器将字节数组还原为指定类型的对象。支持复杂的泛型类型，
     * 通过TypeReference参数指定目标类型。该方法会验证输入参数的有效性，
     * 并在反序列化完成后自动重置Kryo实例。
     * @param bytes   序列化后的字节数组，不能为空或长度为0
     * @param typeRef 目标对象的类型引用，用于指定复杂泛型类型
     * @param <T>     目标对象的泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 当字节数组为空或类型引用为null时抛出
     * @throws ClassCastException 当反序列化后的对象类型与指定类型不匹配时抛出
     */
    public static <T> T deserialize(byte[] bytes, TypeReference<T> typeRef) {
        return deserialize(bytes, typeRef.getType());
    }

    /**
     * 将字节数组反序列化为指定类型的对象（支持多层泛型）
     *
     * 使用Kryo反序列化器将字节数组还原为指定类型的对象。支持多层泛型类型，
     * 通过Type参数指定目标类型。该方法会验证输入参数的有效性，
     * 并在反序列化完成后自动重置Kryo实例。
     * @param bytes 序列化后的字节数组，不能为空或长度为0
     * @param type  目标对象的类型，用于指定反序列化的目标类型
     * @param <T>   目标对象的泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 当字节数组为空或类型为null时抛出
     * @throws ClassCastException 当反序列化后的对象类型与指定类型不匹配时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes, Type type) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Invalid byte array");
        }
        if (type == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }

        Kryo kryo = KYRO_THREAD_LOCAL.get();
        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            // 读取对象
            Object result = kryo.readClassAndObject(input);
            // 获取原始类型
            Class<T> rawType = getRawClass(type);
            // 类型安全检查
            if (!rawType.isInstance(result)) {
                throw new ClassCastException(
                        "Deserialized object is not of type " + rawType.getName()
                                + ". Actual type: " + result.getClass().getName());
            }
            return (T) result;
        } finally {
            // 清理可能存在的循环引用跟踪等状态
            kryo.reset();
        }
    }

    /**
     * 获取Type对应的原始Class类型
     *
     * 根据传入的Type对象获取其对应的原始Class类型。支持Class、ParameterizedType等类型，
     * 并通过缓存机制提升性能。对于不支持的类型会抛出异常。
     * @param type Type对象
     * @param <T>  泛型类型
     * @return Type对应的原始Class类型
     * @throws IllegalArgumentException 当传入不支持的Type类型时抛出
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> getRawClass(Type type) {
        // 从缓存获取
        if (TYPE_TO_CLASS_CACHE.containsKey(type)) {
            return (Class<T>) TYPE_TO_CLASS_CACHE.get(type);
        }
        Class<T> clazz;
        if (type instanceof Class) {
            clazz = (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class<T>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof TypeReference) {
            clazz = getRawClass(((TypeReference<?>) type).getType());
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        // 加入缓存
        TYPE_TO_CLASS_CACHE.put(type, clazz);
        return clazz;
    }

    /**
     * 将字节数组反序列化为指定Class类型的对象
     *
     * 使用Kryo反序列化器将字节数组还原为指定Class类型的对象。这是deserialize方法的简化版本，
     * 适用于不需要处理复杂泛型的场景。该方法会验证输入参数的有效性，
     * 并在反序列化完成后自动重置Kryo实例。
     * @param bytes 序列化后的字节数组，不能为空或长度为0
     * @param clazz 目标对象的Class类型
     * @param <T>   目标对象的泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 当字节数组为空或Class为null时抛出
     * @throws ClassCastException 当反序列化后的对象类型与指定类型不匹配时抛出
     */
    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        return deserialize(bytes, (Type) clazz);
    }

    /**
     * Instant对象的Kryo序列化器
     *
     * 专门用于序列化和反序列化Java 8的Instant时间对象。
     * 序列化时将Instant转换为其秒数和纳秒数进行存储，
     * 反序列化时根据存储的秒数和纳秒数重建Instant对象。
     */
    private static class InstantSerializer extends Serializer<Instant> {

        /**
         * 将Instant对象序列化为字节数据
         *
         * 将Instant对象的epoch秒数和纳秒数写入到输出流中，
         * 以便后续反序列化时能够准确重建Instant对象。
         * @param kryo    Kryo实例
         * @param output  输出流
         * @param instant 待序列化的Instant对象
         */
        @Override
        public void write(Kryo kryo, Output output, Instant instant) {
            // 将 Instant 序列化为秒和纳秒
            output.writeLong(instant.getEpochSecond());
            output.writeInt(instant.getNano());
        }

        /**
         * 从字节数据反序列化为Instant对象
         *
         * 从输入流中读取epoch秒数和纳秒数，并据此重建Instant对象。
         * @param kryo   Kryo实例
         * @param input  输入流
         * @param aClass Instant类类型
         * @return 反序列化后的Instant对象
         */
        @Override
        public Instant read(Kryo kryo, Input input, Class<? extends Instant> aClass) {
            // 从秒和纳秒重建 Instant
            long epochSecond = input.readLong();
            int nano = input.readInt();
            return Instant.ofEpochSecond(epochSecond, nano);
        }

    }

    /**
     * LocalTime对象的Kryo序列化器
     *
     * 专门用于序列化和反序列化Java 8的LocalTime时间对象。
     * 序列化时将LocalTime转换为ISO格式的字符串进行存储，
     * 反序列化时根据存储的字符串重建LocalTime对象。
     */
    private static class LocalTimeSerializer extends Serializer<LocalTime> {

        @Override
        public void write(Kryo kryo, Output output, LocalTime object) {
            output.writeString(object.format(DateTimeFormatter.ISO_LOCAL_TIME));
        }

        @Override
        public LocalTime read(Kryo kryo, Input input, Class<? extends LocalTime> aClass) {
            return LocalTime.parse(input.readString(), DateTimeFormatter.ISO_LOCAL_TIME);
        }

    }

    /**
     * LocalDate 序列化器
     */
    private static class LocalDateSerializer extends Serializer<LocalDate> {

        @Override
        public void write(Kryo kryo, Output output, LocalDate object) {
            output.writeString(object.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        @Override
        public LocalDate read(Kryo kryo, Input input, Class<? extends LocalDate> aClass) {
            return LocalDate.parse(input.readString(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

    }

    /**
     * LocalDateTime 序列化器
     */
    private static class LocalDateTimeSerializer extends Serializer<LocalDateTime> {

        @Override
        public void write(Kryo kryo, Output output, LocalDateTime object) {
            output.writeString(object.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        @Override
        public LocalDateTime read(Kryo kryo, Input input, Class<? extends LocalDateTime> type) {
            return LocalDateTime.parse(input.readString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

    }

    /**
     * ZonedDateTime 序列化器
     */
    private static class ZonedDateTimeSerializer extends Serializer<ZonedDateTime> {

        @Override
        public void write(Kryo kryo, Output output, ZonedDateTime zdt) {
            output.writeString(zdt.toInstant().toString());
            output.writeString(zdt.getZone().getId());
        }

        @Override
        public ZonedDateTime read(Kryo kryo, Input input, Class<? extends ZonedDateTime> type) {
            Instant instant = Instant.parse(input.readString());
            ZoneId zone = ZoneId.of(input.readString());
            return ZonedDateTime.ofInstant(instant, zone);
        }

    }

    private static class OptionalSerializer extends Serializer<Optional<?>> {

        @Override
        public void write(Kryo kryo, Output output, Optional<?> optional) {
            output.writeBoolean(optional.isPresent());
            optional.ifPresent(value -> kryo.writeClassAndObject(output, value));
        }

        @Override
        public Optional<?> read(Kryo kryo, Input input, Class<? extends Optional<?>> type) {
            return input.readBoolean() ? Optional.ofNullable(kryo.readClassAndObject(input)) : Optional.empty();
        }

    }

    private static class CurrencySerializer extends Serializer<Currency> {

        @Override
        public void write(Kryo kryo, Output output, Currency currency) {
            output.writeString(currency.getCurrencyCode());
        }

        @Override
        public Currency read(Kryo kryo, Input input, Class<? extends Currency> type) {
            return Currency.getInstance(input.readString());
        }

    }

    private static class LocaleSerializer extends Serializer<Locale> {

        @Override
        public void write(Kryo kryo, Output output, Locale locale) {
            output.writeString(locale.toLanguageTag());
        }

        @Override
        public Locale read(Kryo kryo, Input input, Class<? extends Locale> type) {
            return Locale.forLanguageTag(input.readString());
        }

    }

    private static class InetAddressSerializer extends Serializer<InetAddress> {

        @Override
        public void write(Kryo kryo, Output output, InetAddress address) {
            output.writeString(address.getHostAddress());
        }

        @Override
        public InetAddress read(Kryo kryo, Input input, Class<? extends InetAddress> type) {
            try {
                return InetAddress.getByName(input.readString());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        // 为避免反射访问内部字段，覆盖copy方法
        @Override
        public InetAddress copy(Kryo kryo, InetAddress original) {
            // InetAddress是不可变的，直接返回原对象
            return original;
        }

    }

    private static class URLSerializer extends Serializer<URL> {

        @Override
        public void write(Kryo kryo, Output output, URL url) {
            output.writeString(url.toString());
        }

        @Override
        public URL read(Kryo kryo, Input input, Class<? extends URL> type) {
            try {
                return new URI(input.readString()).toURL();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        // 为避免反射访问内部字段，覆盖copy方法
        @Override
        public URL copy(Kryo kryo, URL original) {
            // URL是不可变的，直接返回原对象
            return original;
        }

    }

    private static class PatternSerializer extends Serializer<Pattern> {

        @Override
        public void write(Kryo kryo, Output output, Pattern pattern) {
            output.writeString(pattern.pattern());
            output.writeInt(pattern.flags());
        }

        @Override
        public Pattern read(Kryo kryo, Input input, Class<? extends Pattern> type) {
            return Pattern.compile(input.readString(), input.readInt());
        }

    }

}
