package flux;

class Dam {
    final String name;
    final double threshold;
    final double minFlowRate;
    final double maxFlowRate;

    double inflow = 0.0;
    double outflow = 0.0;

    Dam(String name, double threshold, double minFlowRate, double maxFlowRate) {
        this.name = name;
        this.threshold = threshold;
        this.minFlowRate = minFlowRate;
        this.maxFlowRate = maxFlowRate;
    }
}
