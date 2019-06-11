package io.improbable.keanu.tensor.bool;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.improbable.keanu.tensor.Tensor;
import io.improbable.keanu.tensor.TensorShape;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.tensor.intgr.IntegerTensor;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import static com.google.common.primitives.Ints.checkedCast;
import static io.improbable.keanu.tensor.TensorShape.convertFromFlatIndexToPermutedFlatIndex;
import static io.improbable.keanu.tensor.TensorShape.getFlatIndex;
import static io.improbable.keanu.tensor.TensorShape.getPermutationForDimensionToDimensionZero;
import static io.improbable.keanu.tensor.TensorShape.getPermutedIndices;
import static io.improbable.keanu.tensor.TensorShape.getReshapeAllowingWildcard;
import static io.improbable.keanu.tensor.TensorShape.getRowFirstStride;
import static io.improbable.keanu.tensor.TensorShape.getShapeIndices;
import static io.improbable.keanu.tensor.TensorShape.invertedPermute;
import static java.util.Arrays.copyOf;

public class JVMBooleanTensor implements BooleanTensor {

    private final boolean[] data;
    private final long[] shape;
    private final long[] stride;

    /**
     * @param data  tensor data used c ordering
     * @param shape desired shape of tensor
     */
    JVMBooleanTensor(boolean[] data, long[] shape) {
        this.data = data;
        this.shape = shape;
        this.stride = getRowFirstStride(shape);
    }

    JVMBooleanTensor(boolean[] data, long[] shape, long[] stride) {
        this.data = data;
        this.shape = shape;
        this.stride = stride;
    }

    /**
     * @param constant constant boolean value to fill shape
     */
    JVMBooleanTensor(boolean constant) {
        this.data = new boolean[]{constant};
        this.shape = Tensor.SCALAR_SHAPE;
        this.stride = Tensor.SCALAR_STRIDE;
    }

    public static JVMBooleanTensor scalar(boolean scalarValue) {
        return new JVMBooleanTensor(scalarValue);
    }

    public static JVMBooleanTensor create(boolean[] values, long... shape) {
        Preconditions.checkArgument(
            TensorShape.getLength(shape) == values.length,
            "Shape " + Arrays.toString(shape) + " does not match data length " + values.length
        );
        return new JVMBooleanTensor(values, shape);
    }

    public static JVMBooleanTensor create(boolean value, long... shape) {
        boolean[] buffer = new boolean[TensorShape.getLengthAsInt(shape)];

        if (value) {
            Arrays.fill(buffer, value);
        }

        return new JVMBooleanTensor(buffer, shape);
    }

    private boolean[] bufferCopy() {
        return copyOf(data, data.length);
    }

    @Override
    public BooleanTensor reshape(long... newShape) {
        return new JVMBooleanTensor(bufferCopy(), getReshapeAllowingWildcard(shape, data.length, newShape));
    }

    @Override
    public BooleanTensor permute(int... rearrange) {
        Preconditions.checkArgument(rearrange.length == shape.length);
        long[] resultShape = getPermutedIndices(shape, rearrange);
        long[] resultStride = getRowFirstStride(resultShape);
        boolean[] newBuffer = new boolean[data.length];

        for (int flatIndex = 0; flatIndex < data.length; flatIndex++) {

            int permutedFlatIndex = convertFromFlatIndexToPermutedFlatIndex(
                flatIndex,
                shape, stride,
                resultShape, resultStride,
                rearrange
            );

            newBuffer[permutedFlatIndex] = data[flatIndex];
        }

        return new JVMBooleanTensor(newBuffer, resultShape);
    }

    public static BooleanTensor concat(int dimension, BooleanTensor... toConcat) {

        long[] concatShape = TensorShape.getConcatResultShape(dimension, toConcat);

        boolean shouldRearrange = dimension != 0;

        if (shouldRearrange) {

            int[] rearrange = getPermutationForDimensionToDimensionZero(dimension, concatShape);

            BooleanTensor[] toConcatOnDimensionZero = new BooleanTensor[toConcat.length];

            for (int i = 0; i < toConcatOnDimensionZero.length; i++) {
                toConcatOnDimensionZero[i] = toConcat[i].permute(rearrange);
            }

            long[] permutedConcatShape = getPermutedIndices(concatShape, rearrange);
            BooleanTensor concatOnDimZero = concatOnDimensionZero(permutedConcatShape, toConcatOnDimensionZero);

            return concatOnDimZero.permute(invertedPermute(rearrange));
        } else {

            return concatOnDimensionZero(concatShape, toConcat);
        }
    }

    private static JVMBooleanTensor concatOnDimensionZero(long[] concatShape, BooleanTensor... toConcat) {

        boolean[] concatBuffer = new boolean[TensorShape.getLengthAsInt(concatShape)];
        int bufferPosition = 0;

        for (int i = 0; i < toConcat.length; i++) {

            boolean[] cBuffer = getRawBufferIfJVMTensor(toConcat[i]);
            System.arraycopy(cBuffer, 0, concatBuffer, bufferPosition, cBuffer.length);
            bufferPosition += cBuffer.length;
        }

        return new JVMBooleanTensor(concatBuffer, concatShape);
    }

    private static boolean[] getRawBufferIfJVMTensor(BooleanTensor tensor) {
        if (tensor instanceof JVMBooleanTensor) {
            return ((JVMBooleanTensor) tensor).data;
        } else {
            return tensor.asFlatBooleanArray();
        }
    }

    @Override
    public BooleanTensor and(BooleanTensor that) {
        return duplicate().andInPlace(that);
    }

    @Override
    public BooleanTensor or(BooleanTensor that) {
        return duplicate().orInPlace(that);
    }

    @Override
    public BooleanTensor xor(BooleanTensor that) {
        return duplicate().xorInPlace(that);
    }

    @Override
    public BooleanTensor not() {
        return duplicate().notInPlace();
    }

    @Override
    public DoubleTensor doubleWhere(DoubleTensor trueValue, DoubleTensor falseValue) {
        double[] trueValues = trueValue.asFlatDoubleArray();
        double[] falseValues = falseValue.asFlatDoubleArray();

        double[] result = new double[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i] ? getOrScalar(trueValues, i) : getOrScalar(falseValues, i);
        }

        return DoubleTensor.create(result, copyOf(shape, shape.length));
    }

    @Override
    public IntegerTensor integerWhere(IntegerTensor trueValue, IntegerTensor falseValue) {
        FlattenedView<Integer> trueValuesFlattened = trueValue.getFlattenedView();
        FlattenedView<Integer> falseValuesFlattened = falseValue.getFlattenedView();

        int[] result = new int[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i] ? trueValuesFlattened.getOrScalar(i) : falseValuesFlattened.getOrScalar(i);
        }

        return IntegerTensor.create(result, copyOf(shape, shape.length));
    }

    @Override
    public BooleanTensor booleanWhere(BooleanTensor trueValue, BooleanTensor falseValue) {
        FlattenedView<Boolean> trueValuesFlattened = trueValue.getFlattenedView();
        FlattenedView<Boolean> falseValuesFlattened = falseValue.getFlattenedView();

        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i] ? trueValuesFlattened.getOrScalar(i) : falseValuesFlattened.getOrScalar(i);
        }

        return BooleanTensor.create(result, copyOf(shape, shape.length));
    }

    @Override
    public <T, TENSOR extends Tensor<T>> TENSOR where(TENSOR trueValue, TENSOR falseValue) {
        if (trueValue instanceof DoubleTensor && falseValue instanceof DoubleTensor) {
            return (TENSOR) doubleWhere((DoubleTensor) trueValue, (DoubleTensor) falseValue);
        } else if (trueValue instanceof IntegerTensor && falseValue instanceof IntegerTensor) {
            return (TENSOR) integerWhere((IntegerTensor) trueValue, (IntegerTensor) falseValue);
        } else if (trueValue instanceof BooleanTensor && falseValue instanceof BooleanTensor) {
            return (TENSOR) booleanWhere((BooleanTensor) trueValue, (BooleanTensor) falseValue);
        } else {
            FlattenedView<T> trueValuesFlattened = trueValue.getFlattenedView();
            FlattenedView<T> falseValuesFlattened = falseValue.getFlattenedView();

            T[] result = (T[]) (new Object[data.length]);
            for (int i = 0; i < result.length; i++) {
                result[i] = data[i] ? trueValuesFlattened.getOrScalar(i) : falseValuesFlattened.getOrScalar(i);
            }

            return Tensor.create(result, copyOf(shape, shape.length));
        }
    }

    private double getOrScalar(double[] values, int index) {
        if (values.length == 1) {
            return values[0];
        } else {
            return values[index];
        }
    }

    @Override
    public BooleanTensor andInPlace(BooleanTensor that) {
        Boolean[] thatData = that.asFlatArray();
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] && thatData[i];
        }
        return this;
    }

    @Override
    public BooleanTensor orInPlace(BooleanTensor that) {
        Boolean[] thatData = that.asFlatArray();
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] || thatData[i];
        }
        return this;
    }

    @Override
    public BooleanTensor xorInPlace(BooleanTensor that) {
        Boolean[] thatData = that.asFlatArray();
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] ^ thatData[i];
        }
        return this;
    }

    @Override
    public BooleanTensor notInPlace() {
        for (int i = 0; i < data.length; i++) {
            data[i] = !data[i];
        }
        return this;
    }

    @Override
    public boolean allTrue() {
        for (int i = 0; i < data.length; i++) {
            if (!data[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean allFalse() {
        for (int i = 0; i < data.length; i++) {
            if (data[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public DoubleTensor toDoubleMask() {
        double[] doubles = asFlatDoubleArray();
        return DoubleTensor.create(doubles, copyOf(shape, shape.length));
    }

    @Override
    public IntegerTensor toIntegerMask() {
        int[] doubles = asFlatIntegerArray();
        return IntegerTensor.create(doubles, copyOf(shape, shape.length));
    }

    @Override
    public BooleanTensor slice(int dimension, long index) {
        Preconditions.checkArgument(dimension < shape.length && index < shape[dimension]);
        long[] resultShape = ArrayUtils.remove(shape, dimension);
        long[] resultStride = getRowFirstStride(resultShape);
        boolean[] newBuffer = new boolean[TensorShape.getLengthAsInt(resultShape)];

        for (int i = 0; i < newBuffer.length; i++) {

            long[] shapeIndices = ArrayUtils.insert(dimension, getShapeIndices(resultShape, resultStride, i), index);

            int j = Ints.checkedCast(getFlatIndex(shape, stride, shapeIndices));

            newBuffer[i] = data[j];
        }

        return new JVMBooleanTensor(newBuffer, resultShape);
    }

    @Override
    public BooleanTensor take(long... index) {
        return new JVMBooleanTensor(getValue(index));
    }

    @Override
    public int getRank() {
        return shape.length;
    }

    @Override
    public long[] getShape() {
        return Arrays.copyOf(shape, shape.length);
    }

    @Override
    public long[] getStride() {
        return stride;
    }

    @Override
    public long getLength() {
        return data.length;
    }

    @Override
    public BooleanTensor setValue(Boolean value, long... index) {
        data[checkedCast(getFlatIndex(shape, stride, index))] = value;
        return this;
    }

    @Override
    public BooleanTensor duplicate() {
        return new JVMBooleanTensor(copyOf(data, data.length), copyOf(shape, shape.length));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof Tensor) {
            Tensor that = (Tensor) o;
            if (!Arrays.equals(that.getShape(), shape)) return false;
            return Arrays.equals(
                that.asFlatArray(),
                this.asFlatArray()
            );
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(data);
        result = 31 * result + Arrays.hashCode(shape);
        result = 31 * result + Arrays.hashCode(stride);
        return result;
    }

    @Override
    public String toString() {

        StringBuilder dataString = new StringBuilder();
        if (data.length > 20) {
            dataString.append(Arrays.toString(Arrays.copyOfRange(data, 0, 10)));
            dataString.append("...");
            dataString.append(Arrays.toString(Arrays.copyOfRange(data, data.length - 10, data.length)));
        } else {
            dataString.append(Arrays.toString(data));
        }

        return "{\n" +
            "shape = " + Arrays.toString(shape) +
            "\ndata = " + dataString.toString() +
            "\n}";
    }

    @Override
    public FlattenedView<Boolean> getFlattenedView() {
        return new JVMBooleanFlattenedView(data);
    }

    @Override
    public BooleanTensor elementwiseEquals(Boolean value) {
        return Tensor.elementwiseEquals(this, BooleanTensor.create(value, this.getShape()));
    }

    private static class JVMBooleanFlattenedView implements FlattenedView<Boolean> {

        private boolean[] data;

        public JVMBooleanFlattenedView(boolean[] data) {
            this.data = data;
        }

        @Override
        public long size() {
            return data.length;
        }

        @Override
        public Boolean get(long index) {
            return data[Ints.checkedCast(index)];
        }

        @Override
        public Boolean getOrScalar(long index) {
            if (data.length == 1) {
                return get(0);
            } else {
                return get(index);
            }
        }

        @Override
        public void set(long index, Boolean value) {
            data[Ints.checkedCast(index)] = value;
        }

    }

    @Override
    public double[] asFlatDoubleArray() {
        double[] doubles = new double[data.length];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = data[i] ? 1.0 : 0.0;
        }

        return doubles;
    }

    @Override
    public int[] asFlatIntegerArray() {
        int[] integers = new int[data.length];
        for (int i = 0; i < integers.length; i++) {
            integers[i] = data[i] ? 1 : 0;
        }

        return integers;
    }

    @Override
    public Boolean[] asFlatArray() {
        return ArrayUtils.toObject(data);
    }

    @Override
    public boolean[] asFlatBooleanArray() {
        return data;
    }

}
