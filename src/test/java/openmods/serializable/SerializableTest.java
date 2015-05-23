package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import openmods.utils.io.IStreamSerializer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class SerializableTest {

	private final SerializerRegistry registry = new SerializerRegistry();

	public <T> T serializeDeserialize(Class<? extends T> cls, T value) throws IOException {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		registry.writeToStream(output, value);

		ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());
		return registry.createFromStream(input, cls);
	}

	public <T> T testValue(T v) throws IOException {
		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)v.getClass();

		T result = serializeDeserialize(cls, v);
		Assert.assertTrue(cls.isInstance(result));
		Assert.assertEquals(result, v);
		return result;
	}

	public <T> T[] testArray(T[] v) throws IOException {
		@SuppressWarnings("unchecked")
		final Class<? extends T[]> cls = (Class<? extends T[]>)v.getClass();

		T[] result = serializeDeserialize(cls, v);
		Assert.assertTrue(cls.isInstance(result));
		Assert.assertTrue(Arrays.deepEquals(v, result));
		return result;
	}

	public int[] testIntArray(int[] v) throws IOException {
		int[] result = serializeDeserialize(int[].class, v);
		Assert.assertTrue(int[].class.isInstance(result));
		Assert.assertArrayEquals(v, result);
		return result;
	}

	private static IStreamSerializer<TestCls> createSerializer() throws IOException {
		IStreamSerializer<TestCls> serializer = Mockito.mock(TestSerializer.class);

		Mockito.when(serializer.readFromStream(Matchers.any(DataInput.class))).thenReturn(new TestCls());
		return serializer;
	}

	@Test
	public void testInteger() throws IOException {
		testValue(1);
	}

	@Test
	public void testString() throws IOException {
		testValue("hello");
	}

	public static class TestCls {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestCls;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	public interface TestSerializer extends IStreamSerializer<TestCls> {

	}

	@Test
	public void testRegister() throws IOException {
		TestCls testInstance = new TestCls();
		IStreamSerializer<TestCls> serializer = createSerializer();

		registry.register(serializer);

		testValue(testInstance);

		Mockito.verify(serializer).writeToStream(Matchers.eq(testInstance), Matchers.any(DataOutput.class));
		Mockito.verify(serializer).readFromStream(Matchers.any(DataInput.class));
	}

	@Test
	public void testAnonymous() throws IOException {
		TestCls testInstance = new TestCls();

		final IStreamSerializer<TestCls> wrappedSerializer = createSerializer();

		registry.register(new IStreamSerializer<TestCls>() {

			@Override
			public TestCls readFromStream(DataInput input) throws IOException {
				return wrappedSerializer.readFromStream(input);
			}

			@Override
			public void writeToStream(TestCls o, DataOutput output) throws IOException {
				wrappedSerializer.writeToStream(o, output);
			}
		});

		testValue(testInstance);

		Mockito.verify(wrappedSerializer).writeToStream(Matchers.eq(testInstance), Matchers.any(DataOutput.class));
		Mockito.verify(wrappedSerializer).readFromStream(Matchers.any(DataInput.class));
	}

	public class TestSerializable implements IStreamSerializable {

		public TestSerializable(IStreamSerializable delegate) {
			this.delegate = delegate;
		}

		public IStreamSerializable delegate;

		@Override
		public void readFromStream(DataInput input) throws IOException {
			delegate.readFromStream(input);
		}

		@Override
		public void writeToStream(DataOutput output) throws IOException {
			delegate.writeToStream(output);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestSerializable;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	@Test
	public void testClass() throws IOException {
		TestSerializable inputInstance = new TestSerializable(Mockito.mock(IStreamSerializable.class));
		final TestSerializable outputInstance = new TestSerializable(Mockito.mock(IStreamSerializable.class));

		registry.registerSerializable(new IInstanceFactory<TestSerializable>() {
			@Override
			public TestSerializable create() {
				return outputInstance;
			}
		});

		testValue(inputInstance);

		Mockito.verify(inputInstance.delegate).writeToStream(Matchers.any(DataOutput.class));
		Mockito.verify(outputInstance.delegate).readFromStream(Matchers.any(DataInput.class));
	}

	public static enum SingleClassEnum {
		A,
		B,
		C
	}

	public static enum MultipleClassEnum {
		A {},
		B {},
		C {}
	}

	@Test
	public void testEnum() throws IOException {
		testValue(SingleClassEnum.A);
		testValue(SingleClassEnum.B);
		testValue(SingleClassEnum.C);

		testValue(MultipleClassEnum.A);
		testValue(MultipleClassEnum.B);
		testValue(MultipleClassEnum.C);
	}

	@Test
	public void testArrayPrimitive() throws IOException {
		testIntArray(new int[] {});
		testIntArray(new int[] { 1, 2, 3 });
		testIntArray(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
	}

	@Test
	public void testArrayNullable() throws IOException {
		testArray(new String[] {});
		testArray(new String[] { "aa", "", "ccc" });
		testArray(new String[] { null });
		testArray(new String[] { "aa", null, "ccc" });
		testArray(new String[] { "a", "b", "c", "d", "e", "f", "g", "h" });
	}

	@Test
	public void testMultidimensionalArrayNullable() throws IOException {
		testArray(new String[][] {});
		testArray(new String[][] { null });
		testArray(new String[][] { {} });
		testArray(new String[][] { { null } });
		testArray(new String[][] { { "a", "b" }, {}, { "c" } });
		testArray(new String[][] { { "a", "b" }, null, { "c" } });
		testArray(new String[][] { { "a", null }, {}, { "c" } });
	}

	@Test
	public void testMultidimensionalArrayPrimitive() throws IOException {
		testArray(new int[][] {});
		testArray(new int[][] { null });
		testArray(new int[][] { {} });
		testArray(new int[][] { { 1, 2 }, null, { 3 } });
		testArray(new int[][] { { 1, 2 }, {}, { 3 } });
	}

	@Test
	public void testMultidimensionalArrayEnum() throws IOException {
		testArray(new SingleClassEnum[][] {});
		testArray(new SingleClassEnum[][] { null });
		testArray(new SingleClassEnum[][] { {} });
		testArray(new SingleClassEnum[][] { { null } });
		testArray(new SingleClassEnum[][] { { SingleClassEnum.A, SingleClassEnum.B }, null, { SingleClassEnum.C } });
		testArray(new SingleClassEnum[][] { { SingleClassEnum.A, SingleClassEnum.B }, {}, { SingleClassEnum.C } });
	}
}
