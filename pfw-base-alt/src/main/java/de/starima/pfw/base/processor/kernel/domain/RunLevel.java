package de.starima.pfw.base.processor.kernel.domain;

public enum RunLevel {
    BOOTSTRAP(10), //Kernel wird initialisiert
    KERNEL_READY(20), // Kernel ist fertig, der ConstructorIncubator und DescriptorIncubator wird initialisiert
    CONSTRUCTOR_READY(30),
    DESCRIPTOR_READY(40);


    private final int rank;

    RunLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return this.rank;
    }

    public boolean atLeast(RunLevel other) {return  this.getRank() >= other.getRank();}
}