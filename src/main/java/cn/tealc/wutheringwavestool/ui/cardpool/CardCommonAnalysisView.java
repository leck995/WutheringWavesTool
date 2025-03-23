package cn.tealc.wutheringwavestool.ui.cardpool;

import cn.tealc.teafx.utils.AnchorPaneUtil;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

public class CardCommonAnalysisView implements FxmlView<CardCommonAnalysisViewModel>, Initializable {
    @InjectViewModel
    private CardCommonAnalysisViewModel viewModel;

    @FXML
    private Label roleBaseSrCountLabel;

    @FXML
    private Label roleBaseSrNoUpLabel;

    @FXML
    private Label roleBaseSsrAvgLabel;

    @FXML
    private Label roleBaseSsrCountLabel;

    @FXML
    private ListView<SsrData> roleBaseSsrListView;

    @FXML
    private Label roleBaseSsrNoUpLabel;

    @FXML
    private Label roleBaseTimeLabel;

    @FXML
    private Label roleBaseTitleLabel;

    @FXML
    private Label roleBaseTotalTimeLabel;

    @FXML
    private Label roleEventSrCountLabel;

    @FXML
    private Label roleEventSrNoUpLabel;

    @FXML
    private Label roleEventSsrAvgLabel;

    @FXML
    private Label roleEventSsrCountLabel;

    @FXML
    private ListView<SsrData> roleEventSsrListView;

    @FXML
    private Label roleEventSsrNoUpLabel;

    @FXML
    private Label roleEventTimeLabel;

    @FXML
    private Label roleEventTitleLabel;

    @FXML
    private Label roleEventTotalTimeLabel;

    @FXML
    private ListView<SsrData> weaponBaseSsrListView;

    @FXML
    private Label weaponBaseSrCountLabel;

    @FXML
    private Label weaponBaseSrNoUpLabel;

    @FXML
    private Label weaponBaseSsrAvgLabel;

    @FXML
    private Label weaponBaseSsrCountLabel;

    @FXML
    private Label weaponBaseSsrNoUpLabel;

    @FXML
    private Label weaponBaseTimeLabel;

    @FXML
    private Label weaponBaseTitleLabel;

    @FXML
    private Label weaponBaseTotalTimeLabel;

    @FXML
    private Label weaponEventSrCountLabel;

    @FXML
    private Label weaponEventSrNoUpLabel;

    @FXML
    private Label weaponEventSsrAvgLabel;

    @FXML
    private Label weaponEventSsrCountLabel;

    @FXML
    private ListView<SsrData> weaponEventSsrListView;

    @FXML
    private Label weaponEventSsrNoUpLabel;

    @FXML
    private Label weaponEventTimeLabel;

    @FXML
    private Label weaponEventTitleLabel;

    @FXML
    private Label weaponEventTotalTimeLabel;

    @FXML
    private HBox contentPane;

    @FXML
    private StackPane emptyPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        contentPane.visibleProperty().bind(viewModel.emptyProperty().not());
        emptyPane.visibleProperty().bind(viewModel.emptyProperty());

        roleEventTitleLabel.textProperty().bind(viewModel.roleEventTitleLabelProperty());
        roleEventTimeLabel.textProperty().bind(viewModel.roleEventTimeLabelProperty());
        roleEventTotalTimeLabel.textProperty().bind(viewModel.roleEventTotalTimeLabelProperty());
        roleEventSrNoUpLabel.textProperty().bind(viewModel.roleEventSrNoUpLabelProperty());
        roleEventSsrNoUpLabel.textProperty().bind(viewModel.roleEventSsrNoUpLabelProperty());
        roleEventSsrAvgLabel.textProperty().bind(viewModel.roleEventSsrAvgLabelProperty());
        roleEventSsrCountLabel.textProperty().bind(viewModel.roleEventSsrCountLabelProperty());
        roleEventSrCountLabel.textProperty().bind(viewModel.roleEventSrCountLabelProperty());
        roleEventSsrListView.setItems(viewModel.getRoleEventSsrList());
        roleEventSsrListView.setCellFactory(ssrDataListView -> new SsrCell());
        
        
        roleBaseTitleLabel.textProperty().bind(viewModel.roleBaseTitleLabelProperty());
        roleBaseTotalTimeLabel.textProperty().bind(viewModel.roleBaseTotalTimeLabelProperty());
        roleBaseTimeLabel.textProperty().bind(viewModel.roleBaseTimeLabelProperty());
        roleBaseSrNoUpLabel.textProperty().bind(viewModel.roleBaseSrNoUpLabelProperty());
        roleBaseSsrNoUpLabel.textProperty().bind(viewModel.roleBaseSsrNoUpLabelProperty());
        roleBaseSsrAvgLabel.textProperty().bind(viewModel.roleBaseSsrAvgLabelProperty());
        roleBaseSsrCountLabel.textProperty().bind(viewModel.roleBaseSsrCountLabelProperty());
        roleBaseSrCountLabel.textProperty().bind(viewModel.roleBaseSrCountLabelProperty());
        roleBaseSsrListView.setItems(viewModel.getRoleBaseSsrList());
        roleBaseSsrListView.setCellFactory(ssrDataListView -> new SsrCell());


        weaponEventTitleLabel.textProperty().bind(viewModel.weaponEventTitleLabelProperty());
        weaponEventTotalTimeLabel.textProperty().bind(viewModel.weaponEventTotalTimeLabelProperty());
        weaponEventTimeLabel.textProperty().bind(viewModel.weaponEventTimeLabelProperty());
        weaponEventSrNoUpLabel.textProperty().bind(viewModel.weaponEventSrNoUpLabelProperty());
        weaponEventSsrNoUpLabel.textProperty().bind(viewModel.weaponEventSsrNoUpLabelProperty());
        weaponEventSsrAvgLabel.textProperty().bind(viewModel.weaponEventSsrAvgLabelProperty());
        weaponEventSsrCountLabel.textProperty().bind(viewModel.weaponEventSsrCountLabelProperty());
        weaponEventSrCountLabel.textProperty().bind(viewModel.weaponEventSrCountLabelProperty());
        weaponEventSsrListView.setItems(viewModel.getWeaponEventSsrList());
        weaponEventSsrListView.setCellFactory(ssrDataListView -> new SsrCell());


        weaponBaseTitleLabel.textProperty().bind(viewModel.weaponBaseTitleLabelProperty());
        weaponBaseTotalTimeLabel.textProperty().bind(viewModel.weaponBaseTotalTimeLabelProperty());
        weaponBaseTimeLabel.textProperty().bind(viewModel.weaponBaseTimeLabelProperty());
        weaponBaseSrNoUpLabel.textProperty().bind(viewModel.weaponBaseSrNoUpLabelProperty());
        weaponBaseSsrNoUpLabel.textProperty().bind(viewModel.weaponBaseSsrNoUpLabelProperty());
        weaponBaseSsrAvgLabel.textProperty().bind(viewModel.weaponBaseSsrAvgLabelProperty());
        weaponBaseSsrCountLabel.textProperty().bind(viewModel.weaponBaseSsrCountLabelProperty());
        weaponBaseSrCountLabel.textProperty().bind(viewModel.weaponBaseSrCountLabelProperty());
        weaponBaseSsrListView.setItems(viewModel.getWeaponBaseSsrList());
        weaponBaseSsrListView.setCellFactory(ssrDataListView -> new SsrCell());
    }











    class SsrCell extends ListCell<SsrData> {
        public static final String[] COLORS= {"#66cccc","#ff99cc","#1a7f37","#66cc99","#99cc00","#cccccc"};
        private final BorderPane root;
        private ImageView iv;
        private Label name;
        private Label date;
        private Label count;
        private ProgressBar progressBar;
        private Label desc;

        public SsrCell() {
            root=new BorderPane();

            iv = new ImageView();
            iv.setFitHeight(36);
            iv.setFitWidth(36);
            iv.setSmooth(true);


            name = new Label();
            name.getStyleClass().add("role-name");
            date=new Label();
            date.getStyleClass().add("role-date");
            VBox center = new VBox(name,date);
            center.setPadding(new Insets(0,0,0,5));
            center.setAlignment(Pos.CENTER_LEFT);

            desc=new Label();
            desc.getStyleClass().add("role-desc");
            count=new Label();
            count.getStyleClass().add("role-name");
            HBox left = new HBox(5.0,count);
            left.setAlignment(Pos.CENTER_RIGHT);

            progressBar = new ProgressBar();
            progressBar.setProgress(0);
            AnchorPane bottom = new AnchorPane(progressBar);
            AnchorPaneUtil.setPosition(progressBar,0);




            root.setLeft(iv);
            root.setCenter(center);
            root.setRight(left);
            root.setBottom(bottom);

            root.getStyleClass().addAll("role-cell");

            //setDisable(true);

        }

        @Override
        protected void updateItem(SsrData ssrData, boolean b) {
            super.updateItem(ssrData, b);
            if (!b){
                iv.setImage(LocalResourcesManager.header(ssrData.getId() ,60,60));
                name.setText(ssrData.getName());
                date.setText(ssrData.getDate());
                count.setText(String.format("%02d",ssrData.getCount()));
                progressBar.setProgress(ssrData.getCount()/80.0);

                if (ssrData.isEvent()){
                    count.getStyleClass().remove("unup");
                    count.getStyleClass().add("up");
                    progressBar.getStyleClass().remove("unup");
                    progressBar.getStyleClass().add("up");
                }else {
                    count.getStyleClass().remove("up");
                    count.getStyleClass().add("unup");
                    progressBar.getStyleClass().remove("up");
                    progressBar.getStyleClass().add("unup");

                }
                setGraphic(root);
            }else {
                setGraphic(null);
            }
        }

    }

    private class RoleCell extends ListCell<SsrData> {
        private ImageView icon;
        private Label name;
        private Label date;
        private Label count;
        private ProgressBar progressBar;

        public RoleCell() {



        }

        @Override
        protected void updateItem(SsrData ssrData, boolean b) {
            super.updateItem(ssrData, b);
        }
    }
}
