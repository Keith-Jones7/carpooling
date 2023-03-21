package map;

public interface TouringMap<T> {
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

}

