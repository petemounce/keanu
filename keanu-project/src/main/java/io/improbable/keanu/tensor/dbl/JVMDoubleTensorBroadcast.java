package io.improbable.keanu.tensor.dbl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.improbable.keanu.tensor.TensorShape;
import org.nd4j.linalg.api.shape.Shape;

import java.util.Arrays;
import java.util.function.BiFunction;

public class JVMDoubleTensorBroadcast {

    public enum BroadcastableDoubleOperation implements BiFunction<Double, Double, Double> {

        ADD {
            @Override
            public Double apply(Double left, Double right) {
                return left + right;
            }
        },

        SUB {
            @Override
            public Double apply(Double left, Double right) {
                return left - right;
            }
        },

        MUL {
            @Override
            public Double apply(Double left, Double right) {
                return left * right;
            }
        },


        DIV {
            @Override
            public Double apply(Double left, Double right) {
                return left / right;
            }
        },

        GT_MASK {
            @Override
            public Double apply(Double left, Double right) {
                return left > right ? 1.0 : 0.0;
            }
        },

        GTE_MASK {
            @Override
            public Double apply(Double left, Double right) {
                return left >= right ? 1.0 : 0.0;
            }
        },

        LT_MASK {
            @Override
            public Double apply(Double left, Double right) {
                return left < right ? 1.0 : 0.0;
            }
        },

        LTE_MASK {
            @Override
            public Double apply(Double left, Double right) {
                return left <= right ? 1.0 : 0.0;
            }
        }

    }

    static JVMDoubleTensor broadcastScalar(double[] leftBuffer, long[] leftShape, long[] leftStride,
                                           double[] rightBuffer, long[] rightShape, long[] rightStride,
                                           BiFunction<Double, Double, Double> op,
                                           boolean inPlace) {
        final double[] outputBuffer;
        final long[] resultShape;
        final long[] resultStride;

        if (leftShape.length == 0) {
            outputBuffer = new double[rightBuffer.length];
            resultShape = Arrays.copyOf(rightShape, rightShape.length);
            resultStride = Arrays.copyOf(rightStride, rightShape.length);
            scalarLeft(leftBuffer[0], rightBuffer, outputBuffer, op);
        } else {
            outputBuffer = inPlace ? leftBuffer : new double[leftBuffer.length];
            resultShape = Arrays.copyOf(leftShape, leftShape.length);
            resultStride = leftStride;
            scalarRight(leftBuffer, rightBuffer[0], outputBuffer, op);
        }

        return new JVMDoubleTensor(outputBuffer, resultShape, resultStride);
    }

    private static void scalarLeft(double left, double[] rightBuffer, double[] outputBuffer, BiFunction<Double, Double, Double> op) {

        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = op.apply(left, rightBuffer[i]);
        }
    }

    private static void scalarRight(double[] leftBuffer, double right, double[] outputBuffer, BiFunction<Double, Double, Double> op) {

        for (int i = 0; i < leftBuffer.length; i++) {
            outputBuffer[i] = op.apply(leftBuffer[i], right);
        }
    }

    static JVMDoubleTensor elementwiseBinaryOp(double[] leftBuffer, double[] rightBuffer, long[] shape, long[] stride,
                                               BiFunction<Double, Double, Double> op,
                                               boolean inPlace) {

        final double[] outputBuffer = inPlace ? leftBuffer : new double[leftBuffer.length];

        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = op.apply(leftBuffer[i], rightBuffer[i]);
        }

        return new JVMDoubleTensor(outputBuffer, shape, stride);
    }

    static JVMDoubleTensor broadcastBinaryDoubleOp(double[] leftBuffer, long[] leftShape,
                                                   double[] rightBuffer, long[] rightShape,
                                                   BiFunction<Double, Double, Double> op,
                                                   boolean inPlace) {

        //implicitly pad lower ranks with 1s. E.g. [3, 3] & [3] -> [3, 3] -> [1, 3]
        int resultRank = Math.max(leftShape.length, rightShape.length);
        long[] paddedLeftShape = getShapeOrPadToRank(leftShape, resultRank);
        long[] paddedLeftStride = TensorShape.getRowFirstStride(paddedLeftShape);

        long[] paddedRightShape = getShapeOrPadToRank(rightShape, resultRank);
        long[] paddedRightStride = TensorShape.getRowFirstStride(paddedRightShape);

        long[] resultShape = Shape.broadcastOutputShape(paddedLeftShape, paddedRightShape);
        boolean resultShapeIsLeftSideShape = Arrays.equals(resultShape, paddedLeftShape);

        final double[] outputBuffer;
        if (!resultShapeIsLeftSideShape) {

            boolean resultShapeIsRightSideShape = Arrays.equals(resultShape, paddedRightShape);

            if (!resultShapeIsRightSideShape) {
                throw new IllegalArgumentException(
                    "Broadcasting of shape " + Arrays.toString(paddedLeftShape) + " and " + Arrays.toString(paddedRightShape) + " not supported."
                );
            }

            outputBuffer = new double[TensorShape.getLengthAsInt(resultShape)];

        } else {
            outputBuffer = inPlace ? leftBuffer : new double[leftBuffer.length];
        }

        //Allow broadcasting from left and right
        if (paddedLeftShape.length > paddedRightShape.length || leftBuffer.length > rightBuffer.length) {
            //e.g. [2, 2] * [1, 2]
            broadcastFromRight(leftBuffer, paddedLeftStride, rightBuffer, paddedRightShape, paddedRightStride, outputBuffer, op);
        } else {
            //e.g. [2] / [2, 2]
            broadcastFromLeft(leftBuffer, paddedLeftShape, paddedLeftStride, rightBuffer, paddedRightStride, outputBuffer, op);
        }

        return new JVMDoubleTensor(outputBuffer, resultShape);
    }

    private static long[] getShapeOrPadToRank(long[] shape, int rank) {
        if (shape.length == rank) {
            return shape;
        } else {
            return TensorShape.shapeToDesiredRankByPrependingOnes(shape, rank);
        }
    }

    /**
     * Right buffer is shorter than left
     *
     * @param leftBuffer
     * @param leftStride
     * @param rightBuffer
     * @param rightShape
     * @param rightStride
     * @param outputBuffer
     * @param op
     * @return
     */
    static void broadcastFromRight(double[] leftBuffer, long[] leftStride,
                                   double[] rightBuffer, long[] rightShape, long[] rightStride,
                                   double[] outputBuffer, BiFunction<Double, Double, Double> op) {
        Preconditions.checkArgument(leftBuffer.length >= rightBuffer.length);
        for (int i = 0; i < outputBuffer.length; i++) {

            int j = mapBroadcastIndex(i, leftStride, rightShape, rightStride);

            outputBuffer[i] = op.apply(leftBuffer[i], rightBuffer[j]);
        }
    }

    /**
     * Left buffer is shorter than right
     *
     * @param leftBuffer
     * @param leftShape
     * @param leftStride
     * @param rightBuffer
     * @param rightStride
     * @param outputBuffer
     * @param op
     * @return
     */
    static void broadcastFromLeft(double[] leftBuffer, long[] leftShape, long[] leftStride,
                                  double[] rightBuffer, long[] rightStride,
                                  double[] outputBuffer, BiFunction<Double, Double, Double> op) {
        Preconditions.checkArgument(leftBuffer.length <= rightBuffer.length);
        for (int i = 0; i < outputBuffer.length; i++) {

            int j = mapBroadcastIndex(i, rightStride, leftShape, leftStride);

            outputBuffer[i] = op.apply(leftBuffer[j], rightBuffer[i]);
        }
    }

    private static int mapBroadcastIndex(int fromFlatIndex, long[] fromStride, long[] toShape, long[] toStride) {

        final long[] fromShapeIndex = new long[fromStride.length];
        final long[] toShapeIndex = new long[fromShapeIndex.length];
        int remainder = fromFlatIndex;
        int toFlatIndex = 0;

        for (int i = 0; i < fromStride.length; i++) {
            fromShapeIndex[i] = remainder / fromStride[i];
            remainder -= fromShapeIndex[i] * fromStride[i];
            toShapeIndex[i] = fromShapeIndex[i] % toShape[i];
            toFlatIndex += toStride[i] * toShapeIndex[i];
        }

        return toFlatIndex;
    }

}
