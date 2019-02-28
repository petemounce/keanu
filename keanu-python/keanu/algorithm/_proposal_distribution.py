from typing import List, Any, Callable, Dict, Union, cast

from py4j.java_gateway import java_import

from keanu.base import JavaObjectWrapper
from keanu.context import KeanuContext
from keanu.tensor import Tensor
from keanu.vartypes import numpy_types
from keanu.vertex import Vertex

k = KeanuContext()

java_import(k.jvm_view(), "io.improbable.keanu.algorithms.mcmc.proposal.GaussianProposalDistribution")
java_import(k.jvm_view(), "io.improbable.keanu.algorithms.mcmc.proposal.MultivariateGaussianProposalDistribution")
java_import(k.jvm_view(), "io.improbable.keanu.algorithms.mcmc.proposal.PriorProposalDistribution")

proposal_distribution_types: Dict[str, Callable] = {
    "gaussian": k.jvm_view().GaussianProposalDistribution,
    "multivariate_gaussian": k.jvm_view().MultivariateGaussianProposalDistribution,
    "prior": k.jvm_view().PriorProposalDistribution,
}


class ProposalDistribution(JavaObjectWrapper):

    def __init__(self,
                 type_: str,
                 latents: List[Vertex] = None,
                 sigma: Union[numpy_types, List[numpy_types]] = None,
                 listeners: List[Any] = []) -> None:
        ctor = proposal_distribution_types[type_]
        args = []

        if type_ == "gaussian":
            if sigma is None:
                raise TypeError("Gaussian Proposal Distribution requires a value for sigma")
            if type(sigma) == list:
                raise TypeError("Gaussian Proposal Distribution requires single sigma")
            args.append(Tensor(cast(numpy_types, sigma)).unwrap())
        elif type_ == "multivariate_gaussian":
            if sigma is None:
                raise TypeError("Multivariate Gaussian Proposal Distribution requires values for sigma")
            if latents is None:
                raise TypeError("Multivariate Gaussian Proposal Distribution requires latent variables")
            if len(sigma) != len(latents):
                raise TypeError("Multivaraite Gaussian Proposal Distribution requires sigma for each latents")

            sigma_as_tensors = [Tensor(s) for s in sigma]
            args.append(k.to_java_map(dict(zip(latents, sigma_as_tensors))))
        elif sigma is not None:
            raise TypeError('Parameter sigma is not valid unless type is "gaussian" or "multivariate_gaussian"')

        if type_ == "prior":
            if latents is None:
                raise TypeError("Prior Proposal Distribution requires latent variables")

        if len(listeners) > 0:
            args.append(k.to_java_object_list(listeners))
        super(ProposalDistribution, self).__init__(ctor(*args))
