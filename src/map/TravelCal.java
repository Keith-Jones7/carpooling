package map;

public interface TravelCal<T> {

    /**
     * 计算两个元素是否相互包含
     * @param o1    元素1
     * @param o2    元素2
     * @return true 包含，false 不包含
     */
    boolean inEllipsoid(T o1, T o2);

    /**
     * 计算一个元素是否完全被另一个元素包含
     * @param o1    元素1
     * @param o2    元素2
     * @return 元素2是否在元素1中，true 包含，false 不包含
     */
    boolean allInEllipsoid(T o1, T o2);

    /**
     * 计算两个元素的相似度，0-1之间
     * @param o1    元素1
     * @param o2    元素2
     * @return  相似度指标，1表示完全相同
     */
    double calSimilarity(T o1, T o2);
}

