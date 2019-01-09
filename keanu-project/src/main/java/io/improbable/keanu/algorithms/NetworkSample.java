package io.improbable.keanu.algorithms;

import io.improbable.keanu.algorithms.variational.optimizer.Variable;
import io.improbable.keanu.algorithms.variational.optimizer.VariableReference;
import io.improbable.keanu.network.NetworkState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * A network sample contains the state of the network (vertex values) and the logOfMasterP at
 * a given point in time.
 */
@AllArgsConstructor
public class NetworkSample implements NetworkState {

    private final Map<VariableReference, ?> vertexValues;

    @Getter
    private final double logOfMasterP;

    /**
     * @param vertex the vertex to get the values of
     * @param <T>    the type of the values that the vertex contains
     * @return the values of the specified vertex
     */
    @Override
    public <T> T get(Variable<T> vertex) {
        return (T) vertexValues.get(vertex.getReference());
    }

    /**
     * @param vertexId the ID of the vertex to get the values of
     * @param <T>      the type of the values that the vertex contains
     * @return the values of the specified vertex
     */
    @Override
    public <T> T get(VariableReference vertexId) {
        return (T) vertexValues.get(vertexId);
    }

    @Override
    public Set<VariableReference> getVertexIds() {
        return vertexValues.keySet();
    }
}
