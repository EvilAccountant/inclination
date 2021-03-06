package com.ming.inclination.service;

import Filter.Filter;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.ming.inclination.entity.TblFilteredOffset;
import com.ming.inclination.entity.TblOriginOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class FilterService {


    @Value("${fPass}")
    private double fPass;//通带截止频率
    @Value("${fStop}")
    private double fStop;//阻带截止频率
    @Value("${aPass}")
    private double aPass;//通带最大衰减
    @Value("${aStop}")
    private double aStop;//阻带最小衰减
    @Value("${fSample}")
    private double fSample;//采样频率

    /**
     * 将数据滤波并保存
     * X Y数据分开按ID分组，一共 canNumber*2 组数据进行滤波
     *
     * @param offsetList
     * @param filteredList
     */
    public void throughFilter(List<TblOriginOffset> offsetList, List<TblFilteredOffset> filteredList, int canNumber, String[] canIdArr) {
        System.out.println("滤波开始");
        //按ID存放数据
        LinkedHashMap<String, List<Double>> canMapOVX = initCanMap(canNumber, canIdArr);
        LinkedHashMap<String, List<Double>> canMapOVY = initCanMap(canNumber, canIdArr);
        //计数MAP
        HashMap<String, Integer> dataXCount = new HashMap<>(canNumber);
        HashMap<String, Integer> dataYCount = new HashMap<>(canNumber);
        //存储滤波后数据
        List<double[]> tempListX = new ArrayList<>(canNumber);
        List<double[]> tempListY = new ArrayList<>(canNumber);
        double tempDataX, tempDataY;
        String tempId;
        int idTail;
        System.out.println("Step 1========================================");
        for (int i = 0; i < canNumber; i++) {
            dataXCount.put(canIdArr[i], 0);
            dataYCount.put(canIdArr[i], 0);
        }

        System.out.println("Step 2========================================");
        for (TblOriginOffset offset : offsetList) {
            tempId = offset.getCanId();
            tempDataX = offset.getValueX();
            tempDataY = offset.getValueY();
            canMapOVX.get(tempId).add(tempDataX);
            canMapOVY.get(tempId).add(tempDataY);
        }

        System.out.println("Step 3========================================");
        for (List<Double> oriList : canMapOVX.values()) {
            tempListX.add(lowPassFilter(oriList));
        }
        System.out.println("Step 4========================================");
        for (List<Double> oriList : canMapOVY.values()) {
            tempListY.add(lowPassFilter(oriList));
        }

        System.out.println("Step 5========================================");
        for (TblOriginOffset originOffset : offsetList) {
            tempId = originOffset.getCanId();
            idTail = Integer.valueOf(tempId.substring(5)) - 1;

            TblFilteredOffset filteredOffset = new TblFilteredOffset();
            filteredOffset.setCanId(tempId);
            filteredOffset.setDataTime(originOffset.getDataTime());
            filteredOffset.setOriValueX(originOffset.getValueX());
            filteredOffset.setOriValueY(originOffset.getValueY());
            filteredOffset.setValueX(tempListX.get(idTail)[dataXCount.get(tempId)]);
            filteredOffset.setValueY(tempListY.get(idTail)[dataYCount.get(tempId)]);
            //计数map后移一位
            dataXCount.put(tempId, dataXCount.get(tempId) + 1);
            dataYCount.put(tempId, dataYCount.get(tempId) + 1);
            filteredList.add(filteredOffset);
            System.out.println("=================================="+offsetList.size());
        }
        System.out.println("滤波结束");
    }

    /**
     * 滤波处理
     *
     * @param signalList
     * @return
     */
    public double[] lowPassFilter(List<Double> signalList) {
//        double[] signal = signalList.stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
// identity function, Java unboxes automatically to get the double value
        double[] signal = signalList.stream().mapToDouble(d -> d).toArray();
        double[] outData = new double[signal.length];
        Filter filter = null;
        MWNumericArray temp = null;
        try {
            filter = new Filter();
            Object[] result = filter.doFilter(1, fPass, fStop, aPass, aStop, fSample, signal);
            temp = (MWNumericArray) result[0];
            double[][] weights = (double[][]) temp.toDoubleArray();
            System.arraycopy(weights[0], 0, outData, 0, weights[0].length);
        } catch (MWException e) {
            System.out.println("MWException异常");
            e.printStackTrace();
        } catch (Exception e){
            System.out.println("lowPassFilter异常");
            e.printStackTrace();
        }finally {
            if (temp != null) {temp.dispose();}
            if (filter != null) {filter.dispose();}
        }
        return outData;
    }

    /**
     * 初始化各canId的数组Map
     *
     * @return
     */
    private LinkedHashMap<String, List<Double>> initCanMap(int canNumber, String[] canArr) {
        LinkedHashMap<String, List<Double>> canMap = new LinkedHashMap<>(canNumber);
        for (int i = 0; i < canNumber; i++) {
            canMap.put(canArr[i], new ArrayList<>());
        }
        return canMap;
    }
}
