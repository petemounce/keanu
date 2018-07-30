package io.improbable.keanu.vertices.dbl.nonprobabilistic;

import java.util.Map;

import io.improbable.keanu.tensor.NumberTensor;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.dbl.Differentiable;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber;

public class CastDoubleVertex extends NonProbabilisticDouble {

    private final Vertex<? extends NumberTensor> inputVertex;

    public CastDoubleVertex(Vertex<? extends NumberTensor> inputVertex) {
        this.inputVertex = inputVertex;
        setParents(inputVertex);
    }

    @Override
    public DoubleTensor sample(KeanuRandom random) {
        return inputVertex.sample(random).toDouble();
    }

    @Override
    public DoubleTensor getDerivedValue() {
        return inputVertex.getValue().toDouble();
    }

    @Override
    public DualNumber calculateDualNumber(Map<Differentiable, DualNumber> dualNumbers) {
        if (inputVertex instanceof Differentiable) {
            return ((Differentiable) inputVertex).calculateDualNumber(dualNumbers);
        } else {
            throw new UnsupportedOperationException("CastDoubleTensorVertex is non-differentiable");

        }
    }
}
