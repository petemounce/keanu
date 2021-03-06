from typing import Callable, Generator, Dict, Optional, Any, Iterator

from py4j.java_gateway import java_import

from keanu.base import JavaObjectWrapper
from keanu.context import KeanuContext
from keanu.functional import BiConsumer
from keanu.functional import Consumer
from keanu.functional import JavaIterator
from keanu.vertex import Vertex, cast_to_double_vertex, vertex_constructor_param_types
from keanu.vertex.label import _VertexLabel

k = KeanuContext()

java_import(k.jvm_view(), "io.improbable.keanu.templating.SequenceBuilder")
java_import(k.jvm_view(), "io.improbable.keanu.vertices.SimpleVertexDictionary")


class SequenceItem(JavaObjectWrapper):

    def add(self, vertex: Vertex, label: Optional[str] = None) -> None:
        if label is None:
            self.unwrap().add(vertex.unwrap())
        else:
            self.unwrap().add(_VertexLabel(label).unwrap(), vertex.unwrap())

    def get(self, label: str) -> Vertex:
        return Vertex._from_java_vertex(self.unwrap().get(_VertexLabel(label).unwrap()))


class Sequence(JavaObjectWrapper):
    """
    :param factory: a function that takes a :class:`SequenceItem`. Used to add vertices to each SequenceItem.
    :param count: The number of :class:`SequenceItem`s in this sequence.
    :param data_generator: An iterator used to generate the `SequenceItem`s from data.
    Each item in the iterator is a dict, keyed on strings which can be interpreted as variable names.
    Each item is passed to your `factory` function so that you can construct each :class:`SequenceItem`.
    :param initial_state: The starting values of any variables in your sequence. Think of this as "time=0".
    :raises ValueError if you pass in both a count and a data_generator
    :raises ValueError if you pass in neither a count nor a data_generator
    """

    def __init__(self,
                 factory: Callable[..., None],
                 count: int = None,
                 data_generator: Iterator[Dict[str, Any]] = None,
                 initial_state: Dict[str, vertex_constructor_param_types] = None):

        builder = k.jvm_view().SequenceBuilder()

        if initial_state is not None:
            initial_state_java = k.to_java_map(
                {_VertexLabel(k): cast_to_double_vertex(v).unwrap() for (k, v) in initial_state.items()})
            vertex_dictionary = k.jvm_view().SimpleVertexDictionary.backedBy(initial_state_java)
            builder = builder.withInitialState(vertex_dictionary)

        if count is None and data_generator is None:
            raise ValueError(
                "Cannot create a sequence of an unknown size: you must specify either a count of a data_generator")
        elif count is not None and data_generator is not None:
            raise ValueError("If you pass in a data_generator you cannot also pass in a count")

        if count is not None:
            function = lambda p: factory(SequenceItem(p))
            consumer = Consumer(function)
            builder = builder.count(count).withFactory(consumer)

        if data_generator is not None:
            bifunction = lambda p, data: factory(SequenceItem(p), data)
            biconsumer = BiConsumer(bifunction)
            data_generator_java = (k.to_java_map(m) for m in data_generator)
            builder = builder.fromIterator(JavaIterator(data_generator_java)).withFactory(biconsumer)

        sequence = builder.build()
        super().__init__(sequence)

    def __iter__(self) -> Generator[SequenceItem, None, None]:
        iterator = self.unwrap().iterator()
        while iterator.hasNext():
            yield SequenceItem(iterator.next())

    def size(self) -> int:
        return self.unwrap().size()

    @staticmethod
    def proxy_for(label: str) -> str:
        """
        >>> Sequence.proxy_for("foo")
        'proxy_for.foo'
        """
        label_java = _VertexLabel(label).unwrap()
        proxy_label_java = k.jvm_view().SequenceBuilder.proxyFor(label_java)
        return proxy_label_java.getQualifiedName()
