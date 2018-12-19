package io.improbable.keanu.vertices.dbl.nonprobabilistic;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.LoadShape;
import io.improbable.keanu.vertices.NonProbabilistic;
import io.improbable.keanu.vertices.dbl.Differentiable;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;

public class PlaceHolderDoubleVertex extends DoubleVertex implements NonProbabilistic<DoubleTensor>, Differentiable {

    public PlaceHolderDoubleVertex(@LoadShape long[] initialShape) {
        super(initialShape);
    }

    @Override
    public DoubleTensor calculate() {
        return this.getValue();
    }

    @Override
    public DoubleTensor sample(KeanuRandom random) {
        return this.getValue();
    }
}
