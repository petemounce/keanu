package io.improbable.keanu.vertices.dbl.probabilistic;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.VertexId;
import io.improbable.keanu.vertices.dbl.KeanuRandom;

public class SmoothUniformVertexTest {

    private static final double DELTA = 0.0001;

    private KeanuRandom random;

    @Before
    public void setup() {
        random = new KeanuRandom(1);
    }

    @Test
    public void matchesKnownLogDensityOfScalar() {

        SmoothUniformVertex tensorSmoothUniformVertex = new SmoothUniformVertex(0, 1);
        SmoothUniformVertex smoothUniformVertex = new SmoothUniformVertex(0, 1);
        double expectedDensity = smoothUniformVertex.logPdf(0.5);

        ProbabilisticDoubleTensorContract.matchesKnownLogDensityOfScalar(tensorSmoothUniformVertex, 0.5, expectedDensity);
    }

    @Test
    public void matchesKnownLogDensityOfVector() {

        SmoothUniformVertex smoothUniformVertex = new SmoothUniformVertex(0, 1);
        double expectedLogDensity = smoothUniformVertex.logPdf(0.25) + smoothUniformVertex.logPdf(0.75);
        SmoothUniformVertex tensorSmoothUniformVertex = new SmoothUniformVertex(0, 1);

        ProbabilisticDoubleTensorContract.matchesKnownLogDensityOfVector(tensorSmoothUniformVertex,
            new double[]{0.25, 0.75},
            expectedLogDensity
        );
    }

    @Test
    public void matchesKnownDerivativeLogDensityOfScalar() {

        SmoothUniformVertex smoothUniformVertex = new SmoothUniformVertex(0, 1, 10);
        SmoothUniformVertex tensorSmoothUniformVertex = new SmoothUniformVertex(0, 1, 10);

        Map<VertexId, DoubleTensor> derivativeFlatRegion = smoothUniformVertex.dLogPdf(0.5, smoothUniformVertex);
        Map<VertexId, DoubleTensor> tensorDerivativeFlatRegion = tensorSmoothUniformVertex.dLogPdf(0.5, tensorSmoothUniformVertex);

        assertEquals(derivativeFlatRegion.get(smoothUniformVertex.getId()).scalar(),
            tensorDerivativeFlatRegion.get(tensorSmoothUniformVertex.getId()).scalar(),
            DELTA
        );

        Map<VertexId, DoubleTensor> derivativeLeftRegion = smoothUniformVertex.dLogPdf(-0.5, smoothUniformVertex);
        Map<VertexId, DoubleTensor> tensorDerivativeLeftRegion = tensorSmoothUniformVertex.dLogPdf(-0.5, tensorSmoothUniformVertex);

        assertEquals(derivativeLeftRegion.get(smoothUniformVertex.getId()).scalar(),
            tensorDerivativeLeftRegion.get(tensorSmoothUniformVertex.getId()).scalar(),
            DELTA
        );

        Map<VertexId, DoubleTensor> derivativeRightRegion = smoothUniformVertex.dLogPdf(1.5, smoothUniformVertex);
        Map<VertexId, DoubleTensor> tensorDerivativeRightRegion = tensorSmoothUniformVertex.dLogPdf(1.5, tensorSmoothUniformVertex);

        assertEquals(derivativeRightRegion.get(smoothUniformVertex.getId()).scalar(),
            tensorDerivativeRightRegion.get(tensorSmoothUniformVertex.getId()).scalar(),
            DELTA
        );
    }

    @Test
    public void smoothUniformSampleMethodMatchesLogProbMethod() {

        int sampleCount = 1000000;

        double edgeSharpness = 1.0;
        SmoothUniformVertex vertex = new SmoothUniformVertex(
            new int[]{sampleCount, 1},
            0.0,
            1.0,
            edgeSharpness
        );

        double from = -1;
        double to = 2;
        double bucketSize = 0.05;

        ProbabilisticDoubleTensorContract.sampleMethodMatchesLogProbMethod(vertex,
            from,
            to,
            bucketSize,
            1e-2,
            random
        );
    }

}
