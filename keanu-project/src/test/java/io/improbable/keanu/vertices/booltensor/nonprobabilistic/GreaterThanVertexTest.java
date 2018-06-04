package io.improbable.keanu.vertices.booltensor.nonprobabilistic;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.tensor.intgr.IntegerTensor;
import io.improbable.keanu.vertices.booltensor.nonprobabilistic.operators.binary.compare.GreaterThanVertex;
import io.improbable.keanu.vertices.dbltensor.nonprobabilistic.ConstantDoubleTensorVertex;
import io.improbable.keanu.vertices.intgrtensor.nonprobabilistic.ConstantIntegerVertex;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GreaterThanVertexTest {

    @Test
    public void comparesIntegers() {
        isGreaterThan(0, 1, false);
        isGreaterThan(1, 1, false);
        isGreaterThan(2, 1, true);
    }

    @Test
    public void comparesDoubles() {
        isGreaterThan(0.0, 0.5, false);
        isGreaterThan(0.5, 0.5, false);
        isGreaterThan(1.0, 0.5, true);
    }

    private void isGreaterThan(int a, int b, boolean expected) {
        GreaterThanVertex<IntegerTensor, IntegerTensor> vertex = new GreaterThanVertex<>(new ConstantIntegerVertex(a), new ConstantIntegerVertex(b));
        assertEquals(expected, vertex.lazyEval().scalar());
    }

    private void isGreaterThan(double a, double b, boolean expected) {
        GreaterThanVertex<DoubleTensor, DoubleTensor> vertex = new GreaterThanVertex<>(new ConstantDoubleTensorVertex(a), new ConstantDoubleTensorVertex(b));
        assertEquals(expected, vertex.lazyEval().scalar());
    }

}