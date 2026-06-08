package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;

public class CompositeEnvelopeDto implements Serializable {
    private CompositeMetaDto meta;
    private CompositeOperationNodeDto ops;
    private CompositeRouteNodeDto route;

    public CompositeMetaDto getMeta() {
        return meta;
    }

    public CompositeEnvelopeDto setMeta(CompositeMetaDto meta) {
        this.meta = meta;
        return this;
    }

    public CompositeOperationNodeDto getOps() {
        return ops;
    }

    public CompositeEnvelopeDto setOps(CompositeOperationNodeDto ops) {
        this.ops = ops;
        return this;
    }

    public CompositeRouteNodeDto getRoute() {
        return route;
    }

    public CompositeEnvelopeDto setRoute(CompositeRouteNodeDto route) {
        this.route = route;
        return this;
    }
}
