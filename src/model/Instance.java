package model;


import java.util.List;

public class Instance {
    public int nPassengers;
    public int nDrivers;
    public List<Driver> driverList;
    public List<Passenger> passengerList;

    public double[][] ppValidMatrix;
    public double[][] dpTimeMatrix;

    public Instance(List<Driver> driverList, List<Passenger> passengerList, double[][] ppValidMatrix, double[][] dpTimeMatrix) {
        this.nDrivers = driverList.size();
        this.nPassengers = passengerList.size();
        this.driverList = driverList;
        this.passengerList = passengerList;
        this.ppValidMatrix = ppValidMatrix;
        this.dpTimeMatrix = dpTimeMatrix;
        prepare();
    }

    public void prepare() {

    }
}
