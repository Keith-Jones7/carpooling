package model;


import java.util.List;

public class Instance {
    public long cur_time;
    public int match_flag;

    public int nPassengers;
    public int nDrivers;
    public List<Driver> driverList;
    public List<Passenger> passengerList;

    public double[][] ppValidMatrix;
    public double[][] dpValidMatrix;
    public double[][] ppTimeMatrix;
    public double[][] dpTimeMatrix;

    public Instance(long cur_time, int match_flag, List<Driver> driverList, List<Passenger> passengerList, double[][] ppValidMatrix,
                    double[][] dpValidMatrix, double[][] ppTimeMatrix, double[][] dpTimeMatrix) {
        this.cur_time = cur_time;
        this.match_flag = match_flag;
        this.nDrivers = driverList.size();
        this.nPassengers = passengerList.size();
        this.driverList = driverList;
        this.passengerList = passengerList;
        this.ppValidMatrix = ppValidMatrix;
        this.dpValidMatrix = dpValidMatrix;
        this.ppTimeMatrix = ppTimeMatrix;
        this.dpTimeMatrix = dpTimeMatrix;
        prepare();
    }

    public void prepare() {

    }
}
