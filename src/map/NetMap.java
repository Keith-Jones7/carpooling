package map;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;
import common.Param;
import model.Coordinates;
import model.Passenger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NetMap implements TouringMap<Coordinates, Passenger> {
    
    private static final Path MODEL_DIR = Paths.get("MapLearning\\model\\NetMap.pt");
    
    Model model;
    Predictor<double[], Double> predictor;
    public NetMap() {
        model = Model.newInstance("NetMap");
        try {
            model.load(MODEL_DIR);
            predictor = model.newPredictor(new NoBatchifyTranslator<double[], Double>() {
                @Override
                public Double processOutput(TranslatorContext translatorContext, NDList ndList) {
                    return ndList.get(0).getDouble();
                }
                
                @Override
                public NDList processInput(TranslatorContext translatorContext, double[] floats) {
                    NDManager ndManager = translatorContext.getNDManager();
                    NDArray ndArray = ndManager.create(floats);
                    return new NDList(ndArray);
                }
            });
        } catch (MalformedModelException | IOException e) {
            throw new RuntimeException(e);
        }

    }
    

    /**
     * 计算坐标是否重合
     *
     * @param o1 元素1
     * @param o2 元素2
     * @return true: 重合, false: 不重合
     */
    @Override
    public boolean equals(Coordinates o1, Coordinates o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.lat == o2.lat && o1.lng == o2.lng;
    }

    /**
     * 计算两个坐标的空间距离
     *
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离
     */
    @Override
    public double calSpatialDistance(Coordinates o1, Coordinates o2) {
        try {
            return predictor.predict(new double[]{o1.lat, o1.lng, o2.lat, o2.lng});
        } catch (TranslateException e) {
            double lngGap = (o1.lng - o2.lng) * Param.LNG;
            double latGap = (o1.lat - o2.lat) * Param.LAT;
            return Math.sqrt(lngGap * lngGap + latGap * latGap);
        }
    }

    /**
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离基础上的平均行驶时间
     */
    @Override
    public double calTimeDistance(Coordinates o1, Coordinates o2) {
        double dis = calSpatialDistance(o1, o2);
        return dis / Param.SPEED;
    }

    /**
     * 计算两个乘客的椭圆焦点是否相互包含
     *
     * @param p1 乘客1
     * @param p2 乘客2
     * @return 乘客1的终点在乘客2的椭圆内，且乘客2的起点在乘客1的椭圆内
     */

    @Override
    public boolean inEllipsoid(Passenger p1, Passenger p2) {
        double o1_o2 = calTimeDistance(p1.originCoor, p2.originCoor);
        double o2_d1 = calTimeDistance(p2.originCoor, p1.destCoor);
        double d1_d2 = calTimeDistance(p1.destCoor, p2.destCoor);
        return ((o1_o2 + o2_d1 + p1.pastTime) < (p1.expectedArriveTime - p1.submitTime))
                && ((o2_d1 + d1_d2) < (p2.expectedArriveTime - p2.submitTime));
    }

    /**
     * 计算乘客2是否被乘客1的椭圆完全包含
     *
     * @param p1 乘客1
     * @param p2 乘客2
     * @return 乘客2的两个焦点全部在乘客1的椭圆内
     */
    public boolean allInEllipsoid(Passenger p1, Passenger p2) {
        double o1_o2 = calTimeDistance(p1.originCoor, p2.originCoor);
        double o2_d2 = calTimeDistance(p2.originCoor, p2.destCoor);
        double d2_d1 = calTimeDistance(p2.destCoor, p1.destCoor);
        return (o1_o2 + o2_d2 + d2_d1 + p1.pastTime) < (p1.expectedArriveTime - p1.submitTime);
    }

    /**
     * 计算行程相似度
     *
     * @param p1 乘客1
     * @param p2 乘客2
     * @return 返回相同里程，总里程
     */
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
        double similarity = same / (o1_o2 + same + d1_d2);
        if (similarity < Param.MIN_TOURING_SIMILARITY) {
            return 0;
        }
        return similarity;
    }
}
