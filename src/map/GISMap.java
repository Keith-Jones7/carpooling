package map;

import common.Param;
import model.Coordinates;
import model.Passenger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class GISMap implements TouringMap<Coordinates, Passenger> {
    private static final String api = "http://gateway.t3go.com.cn/gis-map-api/lbs/v2/distance/mto";
    private static final CloseableHttpClient httpClient;
    private static final ThreadLocal<HttpPost> localHttpPost = ThreadLocal.withInitial(() -> {
        HttpPost httpPost = new HttpPost(api);
        httpPost.setHeader("Content-Type", "application/json");
        return httpPost;
    });

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10000);
        cm.setDefaultMaxPerRoute(10000);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    private final ConcurrentHashMap<Integer, Double> spatialMap;
    private final ConcurrentHashMap<Integer, Double> timeMap;

    public GISMap() {
        spatialMap = new ConcurrentHashMap<>();
        timeMap = new ConcurrentHashMap<>();
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

    public static String sendHttpPost(String jsonBody) {
        try {
            HttpPost httpPost = localHttpPost.get();
            HttpEntity entity = new StringEntity(jsonBody, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
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
        if (!Param.testMap.inEllipsoid(p1, p2)) {
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
        if (!Param.testMap.allInEllipsoid(p1, p2)) {
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
