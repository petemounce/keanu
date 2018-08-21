package io.improbable.keanu.vertices.dbl.probabilistic;

import static io.improbable.keanu.vertices.dbl.probabilistic.ProbabilisticDoubleTensorContract.moveAlongDistributionAndTestGradientOnARangeOfHyperParameterValues;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.math3.distribution.ParetoDistribution;
import org.junit.Before;
import org.junit.Test;

import io.improbable.keanu.distributions.gradient.Pareto;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.tensor.dbl.Nd4jDoubleTensor;
import io.improbable.keanu.vertices.ConstantVertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.PartialDerivatives;

public class ParetoVertexTest {
    private KeanuRandom random;

    @Before
    public void setup() {
        random = new KeanuRandom(1);
    }

    @Test
    public void matchesKnownLogDensityOfScalar() {
        ParetoDistribution baseline = new ParetoDistribution(1.0, 1.5);
        ParetoVertex vertex = new ParetoVertex(1.0, 1.5);
        double expected = baseline.logDensity(1.25);
        ProbabilisticDoubleTensorContract.matchesKnownLogDensityOfScalar(vertex, 1.25, expected);
    }

    @Test
    public void matchesKnownLogDensityofVector() {
        ParetoDistribution baseline = new ParetoDistribution(1.0, 1.5);
        ParetoVertex vertex = new ParetoVertex(1.0, 1.5);
        double expected = baseline.logDensity(1.25) + baseline.logDensity(6.5);
        ProbabilisticDoubleTensorContract.matchesKnownLogDensityOfVector(vertex, new double[]{1.25, 6.5}, expected);
    }

    @Test
    public void matchesKnownDerivativeLogDensityOfScalar() {
        Pareto.Diff paretoLogDiff = Pareto.dlnPdf(1.0, 1.5, 2.5);

        UniformVertex locationTensor = new UniformVertex(0.0, 1.0);
        locationTensor.setValue(1.0);

        UniformVertex scaleTensor = new UniformVertex(0.0, 2);
        scaleTensor.setValue(1.5);

        ParetoVertex vertex = new ParetoVertex(locationTensor, scaleTensor);
        Map<Long, DoubleTensor> actualDerivatives = vertex.dLogPdf(2.5);
        PartialDerivatives actual = new PartialDerivatives(actualDerivatives);

        assertEquals(paretoLogDiff.dPdXm, actual.withRespectTo(locationTensor.getId()).scalar(), 1e-5);
        assertEquals(paretoLogDiff.dPdAlpha, actual.withRespectTo(scaleTensor.getId()).scalar(), 1e-5);
        assertEquals(paretoLogDiff.dPdX, actual.withRespectTo(vertex.getId()).scalar(), 1e-5);
    }

    @Test
    public void matchesKnownDerivativeLogDensityOfVector() {

        double[] vector = new double[]{1.1, 1.3, 1.8, 2.5, 5};

        UniformVertex locationTensor = new UniformVertex(0.0, 1.0);
        locationTensor.setValue(1.0);

        UniformVertex scaleTensor = new UniformVertex(0.0, 2.0);
        scaleTensor.setValue(1.5);

        Supplier<ParetoVertex> vertexSupplier = () -> new ParetoVertex(locationTensor, scaleTensor);

        ProbabilisticDoubleTensorContract.matchesKnownDerivativeLogDensityOfVector(vector, vertexSupplier);
    }

    @Test
    public void isTreatedAsConstantWhenObserved() {
        UniformVertex xm = new UniformVertex(0.0, 1.0);
        xm.setAndCascade(Nd4jDoubleTensor.scalar(1.0));
        ParetoVertex vertexUnderTest = new ParetoVertex(xm, 3.0);
        vertexUnderTest.setAndCascade(1.0);
        ProbabilisticDoubleTensorContract.isTreatedAsConstantWhenObserved(vertexUnderTest);
        ProbabilisticDoubleTensorContract.hasNoGradientWithRespectToItsValueWhenObserved(vertexUnderTest);
    }

    @Test
    public void dLogProbMatchesFiniteDifferenceCalculationFordPdXm() {
        UniformVertex xm = new UniformVertex(0.0, 1.0);
        ParetoVertex pareto = new ParetoVertex(xm, 3.0);

        DoubleTensor vertexStartValue = Nd4jDoubleTensor.scalar(1.5);
        DoubleTensor vertexEndValue = Nd4jDoubleTensor.scalar(5.0);
        double vertexInc = 0.1;

        moveAlongDistributionAndTestGradientOnARangeOfHyperParameterValues(
            Nd4jDoubleTensor.scalar(0.1),
            Nd4jDoubleTensor.scalar(1.0),
            0.1,
            xm,
            pareto,
            vertexStartValue,
            vertexEndValue,
            vertexInc,
            1e-5
        );
    }

    @Test
    public void dLogProbMatchesFiniteDifferenceCalculationFordPdAlpha() {
        UniformVertex alpha = new UniformVertex(0.1, 5.0);
        ParetoVertex pareto = new ParetoVertex(1.0, alpha);

        DoubleTensor vertexStartValue = Nd4jDoubleTensor.scalar(3.0);
        DoubleTensor vertexEndValue = Nd4jDoubleTensor.scalar(5.0);
        double vertexInc = 0.1;

        moveAlongDistributionAndTestGradientOnARangeOfHyperParameterValues(
            Nd4jDoubleTensor.scalar(0.1),
            Nd4jDoubleTensor.scalar(5.0),
            0.1,
            alpha,
            pareto,
            vertexStartValue,
            vertexEndValue,
            vertexInc,
            0.01
        );
    }

    @Test
    public void sampleMatchesLogProb() {
        int sampleCount = 1000000;
        ParetoVertex vertex = new ParetoVertex(new int[]{sampleCount, 1}, 1.0, 3.0);

        double from = 1.0;
        double to = 2.5;
        double bucketSize = 0.01;

        ProbabilisticDoubleTensorContract.sampleMethodMatchesLogProbMethod(vertex, from, to, bucketSize, 5e-2,
            random);
    }

    @Test
    public void inferHyperParamsFromSamples() {
        double trueXm = 5.0;

        /*
         * Note, this value is set low as the Gradient Optimizer seems to struggle with values greater than this - we
         * should revisit to see if the Optimizer can be Optimzed to deal better with this case.
         */
        double trueAlpha = 0.1;

        List<DoubleVertex> trueParams = new ArrayList<>();
        trueParams.add(ConstantVertex.of(trueXm));
        trueParams.add(ConstantVertex.of(trueAlpha));

        List<DoubleVertex> latentParams = new ArrayList<>();
        UniformVertex latentXm = new UniformVertex(0.1, 15.0);
        latentXm.setAndCascade(6.0);
        UniformVertex latentAlpha = new UniformVertex(0.01, 10);
        latentAlpha.setAndCascade(9.0);
        latentParams.add(latentXm);
        latentParams.add(latentAlpha);

        ParetoVertex paretoVertex = new ParetoVertex(5.0, 2.5);

        int numSamples = 2000;
        VertexVariationalMAP.inferHyperParamsFromSamples(
            hyperParams -> new ParetoVertex(new int[]{numSamples, 1}, hyperParams.get(0), hyperParams.get(1)),
            trueParams,
            latentParams,
            random
        );
    }

    @Test
    public void inferHyperParamsFromSamplesFixedXm() {
        double trueXm = 5.0;
        double trueAlpha = 3.5;

        List<DoubleVertex> trueParams = new ArrayList<>();
        trueParams.add(ConstantVertex.of(trueXm));
        trueParams.add(ConstantVertex.of(trueAlpha));

        List<DoubleVertex> latentParams = new ArrayList<>();
        ConstantDoubleVertex latentXm = new ConstantDoubleVertex(trueXm);
        UniformVertex latentAlpha = new UniformVertex(0.01, 10);
        latentAlpha.setAndCascade(0.5);
        latentParams.add(latentXm);
        latentParams.add(latentAlpha);


        int numSamples = 2000;
        VertexVariationalMAP.inferHyperParamsFromSamples(
            hyperParams -> new ParetoVertex(new int[]{numSamples, 1}, hyperParams.get(0), hyperParams.get(1)),
            trueParams,
            latentParams,
            random
        );
    }

    @Test
    public void inferHyperParamsFromSamplesFixedAlpha() {
        double trueXm = 5.0;
        double trueAlpha = 3.5;

        List<DoubleVertex> trueParams = new ArrayList<>();
        trueParams.add(ConstantVertex.of(trueXm));
        trueParams.add(ConstantVertex.of(trueAlpha));

        List<DoubleVertex> latentParams = new ArrayList<>();
        UniformVertex latentXm = new UniformVertex(0.01, 10);
        latentXm.setAndCascade(10.0);
        ConstantDoubleVertex latentAlpha = new ConstantDoubleVertex(trueAlpha);
        latentParams.add(latentXm);
        latentParams.add(latentAlpha);


        int numSamples = 2000;
        VertexVariationalMAP.inferHyperParamsFromSamples(
            hyperParams -> new ParetoVertex(new int[]{numSamples, 1}, hyperParams.get(0), hyperParams.get(1)),
            trueParams,
            latentParams,
            random
        );
    }
}

