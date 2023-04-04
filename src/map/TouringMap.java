package map;

public interface TouringMap<T, M> {
    /**
     * 判断两个元素是否位置重合
     * @param o1 元素1
     * @param o2 元素2
     * @return true: 重合, false: 不重合
     */
    boolean equals(T o1, T o2);

    /**
     * 计算两个元素之间的距离
     * @param o1 元素1
     * @param o2 元素2
     * @return  返回计算的距离，用浮点数表示
     */

    double calSpatialDistance(T o1, T o2);

    /**
     * 计算两个元素间的时间距离
     * @param o1 元素1
     * @param o2 元素2
     * @return 时间距离
     */
    double calTimeDistance(T o1, T o2);

    /**
     * 计算两个元素是否相互包含
     * @param o1    元素1
     * @param o2    元素2
     * @return true 包含，false 不包含
     */
    boolean inEllipsoid(M o1, M o2);

    /**
     * 计算一个元素是否完全被另一个元素包含
     * @param o1    元素1
     * @param o2    元素2
     * @return 元素2是否在元素1中，true 包含，false 不包含
     */
    boolean allInEllipsoid(M o1, M o2);

    /**
     * 计算两个元素的相似度，0-1之间
     * @param o1    元素1
     * @param o2    元素2
     * @return  相似度指标，1表示完全相同
     */
    double calSimilarity(M o1, M o2);

    /**
     * 
     */
    double calSameTravel(M o1, M o2);

}

