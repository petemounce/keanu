package io.improbable.keanu.vertices.dbl;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.LogProbGraph;
import io.improbable.keanu.vertices.NonProbabilistic;
import io.improbable.keanu.vertices.NonSaveableVertex;

public class DoublePlaceholderVertex extends DoubleVertex implements LogProbGraph.PlaceholderVertex, NonProbabilistic<DoubleTensor>, Differentiable, NonSaveableVertex {

    private final DoubleVertex defaultVertex;

    public DoublePlaceholderVertex(long... initialShape) {
        super(initialShape);
        defaultVertex = null;
    }

    public DoublePlaceholderVertex(DoubleVertex defaultVertex) {
        super(defaultVertex.getShape());
        this.defaultVertex = defaultVertex;
    }

    @Override
    public DoubleTensor calculate() {
        if (hasValue()) {
            return getValue();
        } else if (defaultVertex != null) {
            return defaultVertex.getValue();
        } else {
            throw new IllegalStateException("Placeholders must be fed values");
        }
    }

}