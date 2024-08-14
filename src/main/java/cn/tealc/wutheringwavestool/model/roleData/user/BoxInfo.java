package cn.tealc.wutheringwavestool.model.roleData.user;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxInfo {
        private String boxName;  
        private int num;  
  

        public String getBoxName() {  
            return boxName;  
        }  
  
        public void setBoxName(String boxName) {  
            this.boxName = boxName;  
        }  
  
        public int getNum() {  
            return num;  
        }  
  
        public void setNum(int num) {  
            this.num = num;  
        }  
    }  
  