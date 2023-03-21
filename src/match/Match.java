package match;

import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import model.Driver;
import model.Passenger;
import map.GISMap;

import java.util.List;
public class Match {
    public List<Driver> driverList;
    public List<Passenger> passengerList;

    double[][] valid_matrix;
    int[][] match_matrix;
//    TestMap map = new TestMap();
    private static final map.GISMap map = new GISMap();
    public Match(List<Driver> drivers, List<Passenger> passengers) {
        driverList = drivers;
        passengerList = passengers;
        match_matrix = new int[drivers.size()][passengers.size()];
        valid_matrix = new double[drivers.size()][passengers.size()];
        calValid();
    }

    public void calValid() {
        int i = 0;
        for (Driver driver : driverList) {
            int j = 0;
            for (Passenger passenger : passengerList) {
                long start_time = System.currentTimeMillis();
                if (driver.queue.size() == 0) {
                    //System.out.println(map.calTimeDistance(driver.cur_coor, passenger.origin_coor));
                    if (map.calTimeDistance(driver.cur_coor, passenger.origin_coor) < 300) {
                        valid_matrix[i][j] = 1;
                    }else {
                        valid_matrix[i][j] = 0;
                    }
                }else if (driver.queue.size() == 1){
                    Passenger passenger1 = driver.queue.peek();
                    if (map.inEllipsoid(passenger1, passenger) ||
                            map.allInEllipsoid(passenger1, passenger)) {
                        valid_matrix[i][j] += 1;
                        valid_matrix[i][j] += map.calSimilarity(passenger1, passenger);
                    }else {
                        valid_matrix[i][j] = 0;
                    }
                }
                long end_time = System.currentTimeMillis();
                j++;
            }
            i++;
        }
    }
    public int match(long cur_time) throws Exception {
        IloCplex model = new IloCplex();
        double precision = 1e-5;
        model.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, precision);
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, precision);
        model.setOut(null);
        //System.out.println(model.getVersion());
        int driver_num = driverList.size();
        int passenger_num = passengerList.size();

        //决策变量
        //匹配成功数最大
        IloIntVar[][] match = new IloIntVar[driver_num][passenger_num];
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                match[i][j] = model.intVar(0, 1, "M" + i + "," + j);
            }
        }

        //目标函数
        //将匹配成功数作为最大化目标函数
        IloNumExpr obj = model.intExpr();
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                obj = model.sum(obj, model.prod(match[i][j], valid_matrix[i][j]));
            }
        }
        //将带权路径总和作为最大化目标函数

        model.addMaximize(obj);
        //约束条件

        //一个司机最多匹配一个乘客
        for (int i = 0; i < driver_num; i++) {
            IloIntExpr expr = model.intExpr();
            for (int j = 0; j < passenger_num; j++) {
                expr = model.sum(expr, match[i][j]);
            }
            model.addLe(expr, 1);
        }
        //一个乘客最多匹配一个司机
        for (int j = 0; j < passenger_num; j++) {
            IloIntExpr expr = model.intExpr();
            for (int i = 0; i < driver_num; i++) {
                expr = model.sum(expr, match[i][j]);
            }
            model.addLe(expr, 1);
        }
        //可行约束
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                IloIntExpr expr = model.intExpr();
                expr = model.sum(expr, match[i][j]);
                model.addLe(expr, valid_matrix[i][j]);
            }
        }
        model.solve();
        int count = 0;
        for (int i = driver_num - 1; i >= 0; i--) {
            for (int j = passenger_num - 1; j >= 0; j--) {
                match_matrix[i][j] = (int) model.getValue(match[i][j]);
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
         //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                }
            }
        }
        for (int i = driver_num - 1; i >= 0; i--) {
            if (driverList.get(i).queue.size() == 2) {
                count++;
                driverList.remove(i);
            }
        }
        for (int j = passenger_num - 1; j >= 0; j--) {
            Passenger passenger = passengerList.get(j);
            passenger.renew(cur_time);
            if (passenger.cur_driver != null) {
                passengerList.remove(j);
            }
        }
        return count;
    }
}