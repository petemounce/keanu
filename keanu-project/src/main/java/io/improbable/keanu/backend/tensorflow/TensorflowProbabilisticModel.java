package io.improbable.keanu.backend.tensorflow;

import io.improbable.keanu.algorithms.ProbabilisticModel;
import io.improbable.keanu.algorithms.Variable;
import io.improbable.keanu.algorithms.VariableReference;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.improbable.keanu.backend.ProbabilisticGraphConverter.convertLogProbObservation;
import static io.improbable.keanu.backend.ProbabilisticGraphConverter.convertLogProbPrior;

@AllArgsConstructor
public class TensorflowProbabilisticModel implements ProbabilisticModel {

    public static TensorflowProbabilisticModel convert(BayesianNetwork network) {
        TensorflowComputableGraphBuilder builder = new TensorflowComputableGraphBuilder();

        builder.convert(network.getVertices());

        Optional<VariableReference> logLikelihoodReference = convertLogProbObservation(network, builder);
        VariableReference priorLogProbReference = convertLogProbPrior(network, builder);

        VariableReference logProbReference = logLikelihoodReference
            .map(ll -> builder.add(ll, priorLogProbReference))
            .orElse(priorLogProbReference);

        TensorflowComputableGraph computableGraph = builder.build();

        List<Variable<?, TensorflowVariableState>> latentVariables = builder.getLatentVariables().stream()
            .map(v -> new TensorflowVariable<>(computableGraph, v))
            .collect(Collectors.toList());

        return new TensorflowProbabilisticModel(
            computableGraph,
            latentVariables,
            logProbReference,
            logLikelihoodReference.orElse(null)
        );
    }

    @Getter
    private final TensorflowComputableGraph computableGraph;

    @Getter
    private final List<? extends Variable> latentVariables;

    @Getter
    private final VariableReference logProbOp;

    @Getter
    private final VariableReference logLikelihoodOp;

    @Override
    public double logProb(Map<VariableReference, ?> inputs) {
        DoubleTensor logProb = computableGraph.compute(inputs, logProbOp);
        return logProb.scalar();
    }

    @Override
    public double logLikelihood(Map<VariableReference, ?> inputs) {

        if (logLikelihoodOp == null) {
            throw new IllegalStateException("Likelihood is undefined");
        }

        DoubleTensor logLikelihood = computableGraph.compute(inputs, logLikelihoodOp);
        return logLikelihood.scalar();
    }

    @Override
    public void close() {
        computableGraph.close();
    }
}