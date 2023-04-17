package map;

import common.Param;
import model.Coordinates;
import model.Passenger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class GISMap implements TouringMap<Coordinates, Passenger> {
    private static final String api = "http://gateway.t3go.com.cn/gis-map-api/lbs/v2/distance/mto";
    private final HashMap<Integer, Double> spatialMap;
    private final HashMap<Integer, Double> timeMap;

    public GISMap() {
        spatialMap = new HashMap<>();
        timeMap = new HashMap<>();
    }

    private static String generateJson(Coordinates o, Coordinates d) {
        return "{\"cityCode\": \"320100\",\"dest\": {\"lat\":" +
                String.format("%.6f", d.lat) +
                ",\"lng\":" +
                String.format("%.6f", d.lng) +
                "},\"origins\": [{\"lat\":" +
                String.format("%.6f", o.lat) +
                ",\"lng\":" +
                String.format("%.6f", o.lng) +
                "}]}";
    }

    private static String sendHttpPost(String jsonBody) {
        try {
            URL url = new URL(api);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(jsonBody);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            System.out.println("获取地理位置出错！");
            return "";
        }
    }

    private void addToMap(Coordinates o1, Coordinates o2) {
        int ID = o1.hashCode() + o2.hashCode();
        String post_result = sendHttpPost(generateJson(o1, o2));
        double dist = Double.MAX_VALUE, time = Double.MAX_VALUE;
        String[] splits = post_result.split(":");
        String flag = splits[1].split(",")[0];
        if (flag.equals("\"成功\"")) {
            String distance = splits[8].split(",")[0];
            String duration = splits[9].split("}")[0];
            if (!distance.equals("null")) {
                dist = Double.parseDouble(distance);
                time = Double.parseDouble(duration);
            }
            spatialMap.put(ID, dist);
            timeMap.put(ID, time);
        }
    }

    @Override
    public double calSpatialDistance(Coordinates o1, Coordinates o2) {
        int ID = o1.hashCode() + o2.hashCode();
        if (!spatialMap.containsKey(ID)) {
            addToMap(o1, o2);
        }
        return spatialMap.getOrDefault(ID, Double.MAX_VALUE);
    }

    @Override
    public double calTimeDistance(Coordinates o1, Coordinates o2) {
        int ID = o1.hashCode() + o2.hashCode();
        if (!timeMap.containsKey(ID)) {
            addToMap(o1, o2);
        }
        return timeMap.getOrDefault(ID, Double.MAX_VALUE);
    }

    @Override
    public boolean inEllipsoid(Passenger p1, Passenger p2) {
        if (!Param.inEllipsoid(p1, p2)) {
            return false;
        }
        double o1_o2 = calTimeDistance(p1.curCoor, p2.originCoor);
        double o2_d1 = calTimeDistance(p2.originCoor, p1.destCoor);
        double d1_d2 = calTimeDistance(p1.destCoor, p2.destCoor);
        return o1_o2 + o2_d1 + p1.pastTime < p1.expectedArriveTime - p1.submitTime
                && o2_d1 + d1_d2 < p2.expectedArriveTime - p2.submitTime;
    }

    @Override
    public boolean allInEllipsoid(Passenger p1, Passenger p2) {
        if (!Param.allInEllipsoid(p1, p2)) {
            return false;
        }
        double o1_o2 = calTimeDistance(p1.curCoor, p2.originCoor);
        double o2_d2 = calTimeDistance(p2.originCoor, p2.destCoor);
        double d2_d1 = calTimeDistance(p2.destCoor, p1.destCoor);
        return o1_o2 + o2_d2 + d2_d1 + p1.pastTime < p1.expectedArriveTime - p1.submitTime;
    }

    @Override
    public double calSimilarity(Passenger p1, Passenger p2) {
        if (equals(p1.originCoor, p2.originCoor) && equals(p1.destCoor, p2.destCoor)) {
            return 1;
        }
        double o1_o2 = calSpatialDistance(p1.originCoor, p2.originCoor);
        double o2_d1 = calSpatialDistance(p2.originCoor, p1.destCoor);
        double o2_d2 = calSpatialDistance(p2.originCoor, p2.destCoor);
        double d1_d2 = calSpatialDistance(p1.destCoor, p2.destCoor);
        double same = Math.min(o2_d1, o2_d2);
        return same / (o1_o2 + same + d1_d2);
    }

    @Override
    public boolean equals(Coordinates o1, Coordinates o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.lat == o2.lat && o1.lng == o2.lng;
    }
}
