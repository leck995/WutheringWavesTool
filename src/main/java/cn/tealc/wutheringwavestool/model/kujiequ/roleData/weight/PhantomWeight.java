package cn.tealc.wutheringwavestool.model.kujiequ.roleData.weight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-02 23:08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhantomWeight {
    private String roleName;
    private Map<String,Integer> subPropWeights;





    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Map<String, Integer> getSubPropWeights() {
        return subPropWeights;
    }

    public void setSubPropWeights(Map<String, Integer> subPropWeights) {
        this.subPropWeights = subPropWeights;
    }
}