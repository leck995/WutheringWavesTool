package com.kuro.kujiequ.model.calculator.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CalculatorResult{
        private int roleNum;
        private int weaponNum;
        private TotalCost preview;
        private List<CostList> costList;

        public int getRoleNum() {
                return roleNum;
        }

        public void setRoleNum(int roleNum) {
                this.roleNum = roleNum;
        }

        public int getWeaponNum() {
                return weaponNum;
        }

        public void setWeaponNum(int weaponNum) {
                this.weaponNum = weaponNum;
        }

        public TotalCost getPreview() {
                return preview;
        }

        public void setPreview(TotalCost preview) {
                this.preview = preview;
        }

        public List<CostList> getCostList() {
                return costList;
        }

        public void setCostList(List<CostList> costList) {
                this.costList = costList;
        }
}