package io.improbable.keanu.tensor.dbl;

import com.google.common.primitives.Ints;
import io.improbable.keanu.tensor.TensorShape;
import io.improbable.keanu.tensor.bool.BooleanTensor;
import io.improbable.keanu.tensor.intgr.IntegerTensor;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.FastMath;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.copyOf;

public class JVMDoubleTensor implements DoubleTensor {

    private long[] shape;
    private long[] stride;
    private double[] buffer;

    public static JVMDoubleTensor scalar(double scalarValue) {
        return new JVMDoubleTensor(scalarValue);
    }

    public static JVMDoubleTensor create(double[] values, long[] shape) {
        return new JVMDoubleTensor(values, shape);
    }

    public static JVMDoubleTensor create(double value, long[] shape) {
        long length = TensorShape.getLength(shape);
        double[] buffer = new double[Ints.checkedCast(length)];
        Arrays.fill(buffer, value);
        return new JVMDoubleTensor(buffer, shape);
    }

    public static JVMDoubleTensor ones(long... shape) {
        return create(1.0, shape);
    }

    public static JVMDoubleTensor zeros(long[] shape) {
        return create(0.0, shape);
    }

    public static JVMDoubleTensor eye(long n) {
        double[] buffer = new double[Ints.checkedCast(n * n)];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    buffer[i * j] = 1;
                } else {
                    buffer[i * j] = 0;
                }
            }
        }
        return new JVMDoubleTensor(buffer, new long[]{n, n});
    }

    public JVMDoubleTensor(double value) {
        this.shape = new long[0];
        this.stride = new long[0];
        this.buffer = new double[]{value};
    }

    private JVMDoubleTensor(double[] data, long[] shape) {
        this.shape = shape;
        this.stride = TensorShape.getRowFirstStride(shape);
        this.buffer = data;
    }

    private double[] newBuffer() {
        return new double[buffer.length];
    }

    private double[] bufferCopy() {
        return copyOf(buffer, buffer.length);
    }

    private int[] bufferAsInteger() {
        int[] intBuffer = new int[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            intBuffer[i] = (int) buffer[i];
        }
        return intBuffer;
    }

    private long[] shapeCopy() {
        return copyOf(shape, shape.length);
    }

    private void checkElementwiseShapeMatch(long[] otherShape) {
        if (!Arrays.equals(shape, otherShape)) {
            throw new IllegalArgumentException(
                "Broadcast not supported for shape " +
                    Arrays.toString(shape) +
                    " and " +
                    Arrays.toString(otherShape)
            );
        }
    }

    @Override
    public int getRank() {
        return shape.length;
    }

    @Override
    public long[] getShape() {
        return shape;
    }

    @Override
    public long getLength() {
        return buffer.length > 0 ? buffer.length : 1;
    }

    @Override
    public Double getValue(long... index) {
        long flatIndex = TensorShape.getFlatIndex(shape, stride, index);
        return buffer[Ints.checkedCast(flatIndex)];
    }

    @Override
    public DoubleTensor setValue(Double value, long... index) {
        long flatIndex = TensorShape.getFlatIndex(shape, stride, index);
        buffer[Ints.checkedCast(flatIndex)] = value;
        return this;
    }

    @Override
    public Double scalar() {
        return buffer[0];
    }

    @Override
    public DoubleTensor reshape(long... newShape) {
        return new JVMDoubleTensor(copyOf(buffer, buffer.length), copyOf(newShape, newShape.length));
    }

    @Override
    public FlattenedView<Double> getFlattenedView() {
        return null;
    }

    @Override
    public BooleanTensor elementwiseEquals(Double value) {
        return null;
    }

    @Override
    public DoubleTensor permute(int... rearrange) {
        throw new NotImplementedException();
    }

    @Override
    public DoubleTensor duplicate() {
        return new JVMDoubleTensor(copyOf(buffer, buffer.length), shapeCopy());
    }

    @Override
    public Double sum() {
        double result = 0;
        for (int i = 0; i < buffer.length; i++) {
            result += buffer[i];
        }
        return result;
    }

    @Override
    public DoubleTensor toDouble() {
        return duplicate();
    }

    @Override
    public IntegerTensor toInteger() {
        return IntegerTensor.create(bufferAsInteger(), shapeCopy());
    }

    @Override
    public DoubleTensor diag() {
        return null;
    }

    @Override
    public DoubleTensor transpose() {
        return null;
    }

    @Override
    public DoubleTensor sum(int... overDimensions) {
        return null;
    }

    @Override
    public DoubleTensor reciprocal() {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = 1.0 / buffer[i];
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor minus(double value) {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = buffer[i] - value;
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor plus(double value) {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = buffer[i] + value;
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor div(double value) {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = buffer[i] / value;
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor matrixMultiply(DoubleTensor value) {
        return null;
    }

    @Override
    public DoubleTensor tensorMultiply(DoubleTensor value, int[] dimsLeft, int[] dimsRight) {
        return null;
    }

    @Override
    public DoubleTensor abs() {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = Math.abs(buffer[i]);
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public int argMax() {
        return 0;
    }

    @Override
    public IntegerTensor argMax(int axis) {
        return null;
    }

    @Override
    public DoubleTensor getGreaterThanMask(DoubleTensor greaterThanThis) {
        return null;
    }

    @Override
    public DoubleTensor getGreaterThanOrEqualToMask(DoubleTensor greaterThanThis) {
        return null;
    }

    @Override
    public DoubleTensor getLessThanMask(DoubleTensor lessThanThis) {
        return null;
    }

    @Override
    public DoubleTensor getLessThanOrEqualToMask(DoubleTensor lessThanThis) {
        return null;
    }

    @Override
    public DoubleTensor setWithMaskInPlace(DoubleTensor mask, Double value) {
        return null;
    }

    @Override
    public DoubleTensor setWithMask(DoubleTensor mask, Double value) {
        return null;
    }

    @Override
    public DoubleTensor apply(Function<Double, Double> function) {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = function.apply(buffer[i]);
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor minusInPlace(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] - thatBuffer[i];
        }

        return this;
    }

    @Override
    public DoubleTensor plusInPlace(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] + thatBuffer[i];
        }

        return this;
    }

    @Override
    public DoubleTensor divInPlace(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] / thatBuffer[i];
        }

        return this;
    }

    @Override
    public DoubleTensor powInPlace(DoubleTensor exponent) {
        checkElementwiseShapeMatch(exponent.getShape());

        double[] exponentBuffer = exponent.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.pow(buffer[i], exponentBuffer[i]);
        }

        return this;
    }

    @Override
    public DoubleTensor unaryMinusInPlace() {

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = -buffer[i];
        }

        return this;
    }

    @Override
    public DoubleTensor absInPlace() {

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Math.abs(buffer[i]);
        }

        return this;
    }

    @Override
    public DoubleTensor applyInPlace(Function<Double, Double> function) {

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = function.apply(buffer[i]);
        }

        return this;
    }

    @Override
    public BooleanTensor lessThan(DoubleTensor value) {
        return null;
    }

    @Override
    public BooleanTensor lessThanOrEqual(DoubleTensor value) {
        return null;
    }

    @Override
    public BooleanTensor greaterThan(DoubleTensor value) {
        return null;
    }

    @Override
    public BooleanTensor greaterThanOrEqual(DoubleTensor value) {
        return null;
    }

    @Override
    public DoubleTensor pow(DoubleTensor exponent) {
        checkElementwiseShapeMatch(exponent.getShape());

        double[] exponentBuffer = exponent.asFlatDoubleArray();
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.pow(buffer[i], exponentBuffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor pow(double exponent) {

        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.pow(buffer[i], exponent);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor sqrt() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.sqrt(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor log() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.log(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor safeLogTimes(DoubleTensor y) {
        return null;
    }

    @Override
    public DoubleTensor logGamma() {

        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = Gamma.logGamma(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor digamma() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = Gamma.digamma(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor sin() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.sin(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor cos() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.cos(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor tan() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.tan(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor atan() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.atan(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor atan2(double y) {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.atan2(y, buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor atan2(DoubleTensor y) {
        return null;
    }

    @Override
    public DoubleTensor asin() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.asin(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor acos() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.acos(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor exp() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.exp(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor matrixInverse() {
        return null;
    }

    @Override
    public double max() {
        double result = -Double.MAX_VALUE;
        for (int i = 0; i < buffer.length; i++) {
            result = Math.max(result, buffer[i]);
        }
        return result;
    }

    @Override
    public double min() {
        double result = Double.MAX_VALUE;
        for (int i = 0; i < buffer.length; i++) {
            result = Math.min(result, buffer[i]);
        }
        return result;
    }

    @Override
    public double average() {
        return 0;
    }

    @Override
    public double standardDeviation() {
        return 0;
    }

    @Override
    public boolean equalsWithinEpsilon(DoubleTensor other, double epsilon) {
        return false;
    }

    @Override
    public DoubleTensor standardize() {
        return null;
    }

    @Override
    public DoubleTensor replaceNaN(double value) {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = Double.isNaN(buffer[i]) ? value : buffer[i];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor clamp(DoubleTensor min, DoubleTensor max) {
        double[] newBuffer = newBuffer();
        double[] minBuffer = min.asFlatDoubleArray();
        double[] maxBuffer = max.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = Math.max(minBuffer[i], Math.min(maxBuffer[i], buffer[i]));
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor ceil() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.ceil(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor floor() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.floor(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor round() {
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = FastMath.round(buffer[i]);
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor sigmoid() {
        return null;
    }

    @Override
    public DoubleTensor choleskyDecomposition() {
        return null;
    }

    @Override
    public double determinant() {
        return 0;
    }

    @Override
    public double product() {
        double result = 0;
        for (int i = 0; i < buffer.length; i++) {
            result *= buffer[i];
        }
        return result;
    }

    @Override
    public DoubleTensor slice(int dimension, long index) {
        return null;
    }

    @Override
    public double[] asFlatDoubleArray() {
        return bufferCopy();
    }

    @Override
    public int[] asFlatIntegerArray() {
        return bufferAsInteger();
    }

    @Override
    public Double[] asFlatArray() {
        Double[] boxedBuffer = new Double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            boxedBuffer[i] = buffer[i];
        }
        return boxedBuffer;
    }

    @Override
    public List<DoubleTensor> split(int dimension, long... splitAtIndices) {
        return null;
    }

    @Override
    public DoubleTensor reciprocalInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 1.0 / buffer[i];
        }
        return this;
    }

    @Override
    public DoubleTensor minusInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] - value;
        }
        return this;
    }

    @Override
    public DoubleTensor plusInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] + value;
        }
        return this;
    }

    @Override
    public DoubleTensor divInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] / value;
        }
        return this;
    }

    @Override
    public DoubleTensor powInPlace(double exponent) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.pow(buffer[i], exponent);
        }
        return this;
    }

    @Override
    public DoubleTensor sqrtInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.sqrt(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor logInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.log(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor safeLogTimesInPlace(DoubleTensor y) {
        return null;
    }

    @Override
    public DoubleTensor logGammaInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Gamma.logGamma(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor digammaInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Gamma.digamma(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor sinInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.sin(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor cosInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.cos(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor tanInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.tan(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor atanInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.atan(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor atan2InPlace(double y) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.atan2(y, buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor atan2InPlace(DoubleTensor y) {
        checkElementwiseShapeMatch(y.getShape());

        double[] thatBuffer = y.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.atan2(thatBuffer[i], buffer[i]);
        }

        return this;
    }

    @Override
    public DoubleTensor asinInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.asin(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor acosInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.acos(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor expInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.exp(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor minInPlace(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Math.min(buffer[i], thatBuffer[i]);
        }

        return this;
    }

    @Override
    public DoubleTensor maxInPlace(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Math.max(buffer[i], thatBuffer[i]);
        }

        return this;
    }

    @Override
    public DoubleTensor clampInPlace(DoubleTensor min, DoubleTensor max) {

        double[] minBuffer = min.asFlatDoubleArray();
        double[] maxBuffer = max.asFlatDoubleArray();

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Math.max(minBuffer[i], Math.min(maxBuffer[i], buffer[i]));
        }

        return this;
    }

    @Override
    public DoubleTensor ceilInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.ceil(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor floorInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.floor(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor roundInPlace() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = FastMath.round(buffer[i]);
        }
        return this;
    }

    @Override
    public DoubleTensor sigmoidInPlace() {
        return null;
    }

    @Override
    public DoubleTensor standardizeInPlace() {
        return null;
    }

    @Override
    public DoubleTensor replaceNaNInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Double.isNaN(buffer[i]) ? value : buffer[i];
        }
        return this;
    }

    @Override
    public DoubleTensor setAllInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = value;
        }
        return this;
    }

    @Override
    public BooleanTensor lessThan(double value) {
        boolean[] newBuffer = new boolean[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] < value;
        }

        return BooleanTensor.create(newBuffer, shapeCopy());
    }

    @Override
    public BooleanTensor lessThanOrEqual(double value) {
        boolean[] newBuffer = new boolean[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] <= value;
        }

        return BooleanTensor.create(newBuffer, shapeCopy());
    }

    @Override
    public BooleanTensor greaterThan(double value) {
        boolean[] newBuffer = new boolean[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] > value;
        }

        return BooleanTensor.create(newBuffer, shapeCopy());
    }

    @Override
    public BooleanTensor greaterThanOrEqual(double value) {
        boolean[] newBuffer = new boolean[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] >= value;
        }

        return BooleanTensor.create(newBuffer, shapeCopy());
    }

    @Override
    public BooleanTensor notNaN() {
        boolean[] newBuffer = new boolean[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = Double.isNaN(buffer[i]);
        }

        return BooleanTensor.create(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor minus(DoubleTensor that) {

        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] - thatBuffer[i];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor plus(DoubleTensor that) {

        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] + thatBuffer[i];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor timesInPlace(double value) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] * value;
        }
        return this;
    }

    @Override
    public DoubleTensor times(double value) {
        double[] result = new double[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            result[i] = buffer[i] * value;
        }
        return new JVMDoubleTensor(result, shapeCopy());
    }

    @Override
    public DoubleTensor timesInPlace(DoubleTensor that) {

        //ensure left is highest rank
        if (that.getRank() > this.getRank()) {
            return that.times(this);
        }

        double[] thatBuffer = that.asFlatDoubleArray();
        int thatLength = thatBuffer.length;

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] * thatBuffer[i % thatLength];
        }

        return this;
    }

    @Override
    public DoubleTensor times(DoubleTensor that) {

        //ensure left is highest rank
        if (that.getRank() > this.getRank()) {
            return that.times(this);
        }

        double[] thatBuffer = that.asFlatDoubleArray();
        double[] newBuffer = newBuffer();

        int thatLength = thatBuffer.length;

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] * thatBuffer[i % thatLength];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor div(DoubleTensor that) {
        checkElementwiseShapeMatch(that.getShape());

        double[] thatBuffer = that.asFlatDoubleArray();
        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i] / thatBuffer[i];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public DoubleTensor unaryMinus() {

        double[] newBuffer = newBuffer();

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = -buffer[i];
        }

        return new JVMDoubleTensor(newBuffer, shapeCopy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JVMDoubleTensor that = (JVMDoubleTensor) o;
        return Arrays.equals(shape, that.shape) &&
            Arrays.equals(buffer, that.buffer);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(shape);
        result = 31 * result + Arrays.hashCode(buffer);
        return result;
    }
}