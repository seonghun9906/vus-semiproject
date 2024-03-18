package kr.co.icia.vrp.semi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.icia.vrp.semi.entity.Node;
import kr.co.icia.vrp.semi.entity.NodeCost;
import kr.co.icia.vrp.semi.parameter.NodeCostParam;
import kr.co.icia.vrp.semi.service.NodeCostService;
import kr.co.icia.vrp.semi.service.NodeService;
import kr.co.icia.vrp.semi.util.JsonResult;
import kr.co.icia.vrp.semi.util.KakaoApiUtil;
import kr.co.icia.vrp.semi.util.KakaoApiUtil.Point;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections.Route;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections.Route.Section;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections.Route.Section.Road;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections.Route.Summary;
import kr.co.icia.vrp.semi.util.kakao.KakaoDirections.Route.Summary.Fare;
import kr.co.icia.vrp.semi.vrp.VrpResult;
import kr.co.icia.vrp.semi.vrp.VrpService;
import kr.co.icia.vrp.semi.vrp.VrpVehicleRoute;

@Controller
public class MainController {
  @Autowired
  private NodeService nodeService;
  @Autowired
  private NodeCostService nodeCostService;

  @GetMapping("/main")
  public String getMain() {
    return "main"; // html의 위치와 파일로 단순히 리턴
  }

  @GetMapping("/poi") // "/poi" 경로로의 GET 요청을 처리하는 메소드
  @ResponseBody // 반환되는 객체를 HTTP 응답 본문으로 직접 사용하도록 지정 , ajax json으로 받을때 자동으로 받아준다.
  public JsonResult getPoi(@RequestParam double x, @RequestParam double y) throws IOException, InterruptedException {
      // 클라이언트로부터 받은 x와 y 좌표로 중심 좌표를 생성
      Point center = new Point(x, y);

      // 카카오 API를 사용하여 "약국" 키워드로 주변 약국 위치를 가져옴
      List<Point> pointList = KakaoApiUtil.getPointByKeyword("약국", center);

      List<Node> nodeList = new ArrayList<>(); // 주변 약국 노드 리스트

      // 주변 약국 리스트 만들기
      for (Point point : pointList) {
          // 해당 위치의 약국 노드를 데이터베이스에서 가져오거나, 없으면 새로 생성하여 추가
          Node node = nodeService.getOne(Long.valueOf(point.getId()));
          if (node == null) {
              node = new Node();
              node.setId(Long.valueOf(point.getId())); // 노드 id
              node.setName(point.getName()); //이름
              node.setPhone(point.getPhone()); // 전화번호
              node.setAddress(point.getAdddress()); // 주소
              node.setX(point.getX()); // 경도
              node.setY(point.getY()); // 위도
              node.setRegDt(new Date()); // 등록일시
              node.setModDt(new Date()); // 수정일시
              nodeService.add(node); // 데이터베이스에 노드 추가
          }
          nodeList.add(node); // 노드 리스트에 추가
      }

      int totalDistance = 0; // 전체 이동 거리
      int totalDuration = 0; // 전체 이동 시간
      List<Point> totalPathPointList = new ArrayList<>(); // 전체 이동 경로 포인트 리스트

      // 노드 간 이동 거리 및 시간 계산
      for (int i = 1; i < nodeList.size(); i++) {
          Node prev = nodeList.get(i - 1); //앞 노드 계산방식
          Node next = nodeList.get(i); //다음 노드 계산방식

          // 노드 간 이동 거리 및 시간 정보 가져오기
          NodeCost nodeCost = getNodeCost(prev, next);

          // 만약 이동 거리 및 시간 정보가 없다면 다음 노드로 진행
          if (nodeCost == null) {
              continue;
          }
          totalDistance += nodeCost.getDistanceMeter(); // 이동 거리 합산
          totalDuration += nodeCost.getDurationSecond(); // 이동 시간 합산

          // 이동 경로 포인트 리스트에 추가
          totalPathPointList.addAll(new ObjectMapper().readValue(nodeCost.getPathJson(), new TypeReference<List<Point>>() {}));
      }

      // 결과를 담을 JsonResult 객체 생성
      JsonResult jsonResult = new JsonResult();
      jsonResult.addData("totalDistance", totalDistance); // 전체 이동 거리 추가
      jsonResult.addData("totalDuration", totalDuration); // 전체 이동 시간 추가
      jsonResult.addData("totalPathPointList", totalPathPointList); // 전체 이동 경로 추가
      jsonResult.addData("nodeList", nodeList); // 방문지 목록 추가

      return jsonResult; // JsonResult 객체 반환
  }

  @PostMapping("/vrp") // "/vrp" 경로로의 POST 요청을 처리하는 메소드
  @ResponseBody // 반환되는 객체를 HTTP 응답 본문으로 직접 사용하도록 지정
  public JsonResult postVrp(@RequestBody List<Node> nodeList) throws IOException, InterruptedException {
      // VRP 서비스 생성
      VrpService vrpService = new VrpService();

      // 첫 번째 노드를 가져와서 차량 등록을 위해 id 추출
      // 경로 설정이 최적화가 안된 노드 리스트
      Node firstNode = nodeList.get(0);
      String firstNodeId = String.valueOf(firstNode.getId());

      // 차량 등록
      vrpService.addVehicle("차량01", firstNodeId);

      // 노드 및 경로 관련 맵 초기화
      Map<String, Node> nodeMap = new HashMap<>();
      Map<String, Map<String, NodeCost>> nodeCostMap = new HashMap<>();

      // 노드 리스트 순회
      for (Node node : nodeList) {
          String nodeId = String.valueOf(node.getId());
          // 화물 등록
          vrpService.addShipement(node.getName(), firstNodeId, nodeId);
          nodeMap.put(nodeId, node); // 노드 맵에 추가
      }

      // 노드 간 이동 거리 및 시간 계산
      for (int i = 0; i < nodeList.size(); i++) {
          Node startNode = nodeList.get(i);
          for (int j = 0; j < nodeList.size(); j++) {
              Node endNode = nodeList.get(j);
              NodeCost nodeCost = getNodeCost(startNode, endNode);
              if (i == j) {
                  continue;
              }
              if (nodeCost == null) {
                  nodeCost = new NodeCost();
                  nodeCost.setDistanceMeter(0L);
                  nodeCost.setDurationSecond(0L);
              }
              Long distanceMeter = nodeCost.getDistanceMeter();
              Long durationSecond = nodeCost.getDurationSecond();
              String startNodeId = String.valueOf(startNode.getId());
              String endNodeId = String.valueOf(endNode.getId());
              // 비용 등록
              vrpService.addCost(startNodeId, endNodeId, durationSecond, distanceMeter);
              if (!nodeCostMap.containsKey(startNodeId)) {
                  nodeCostMap.put(startNodeId, new HashMap<>());
              }
              nodeCostMap.get(startNodeId).put(endNodeId, nodeCost);
          }
      }

      // VRP 결과 처리를 위한 리스트 초기화
      List<Node> vrpNodeList = new ArrayList<>();
      //경로최적화가 된 노드 리스트
      
      
      VrpResult vrpResult = vrpService.getVrpResult();

      // VRP 결과에서 노드 정보 추출
      for (VrpVehicleRoute vrpVehicleRoute : vrpResult.getVrpVehicleRouteList()) {
          if ("deliverShipment".equals(vrpVehicleRoute.getActivityName())) {
              String locationId = vrpVehicleRoute.getLocationId();
              vrpNodeList.add(nodeMap.get(locationId));
          }
          System.out.println(vrpVehicleRoute);
      }

      // 전체 이동 거리, 시간 및 경로 계산
      int totalDistance = 0;
      int totalDuration = 0;
      List<Point> totalPathPointList = new ArrayList<>();
      for (int i = 1; i < vrpNodeList.size(); i++) {
          Node prev = vrpNodeList.get(i - 1);
          Node next = vrpNodeList.get(i);
          NodeCost nodeCost = nodeCostMap.get(String.valueOf(prev.getId())).get(String.valueOf(next.getId()));
          if (nodeCost == null) {
              continue;
          }
          totalDistance += nodeCost.getDistanceMeter();
          totalDuration += nodeCost.getDurationSecond();
          String pathJson = nodeCost.getPathJson();
          if (pathJson != null) {
              totalPathPointList.addAll(new ObjectMapper().readValue(pathJson, new TypeReference<List<Point>>() {}));
          }
      }

      // 결과를 담을 JsonResult 객체 생성
      JsonResult jsonResult = new JsonResult();
      jsonResult.addData("totalDistance", totalDistance); // 전체 이동 거리 추가
      jsonResult.addData("totalDuration", totalDuration); // 전체 이동 시간 추가
      jsonResult.addData("totalPathPointList", totalPathPointList); // 전체 이동 경로 추가
      jsonResult.addData("nodeList", vrpNodeList); // 방문지 목록 추가

      return jsonResult; // JsonResult 객체 반환
  }

  // 두 노드 사이의 이동 거리 및 시간을 가져오는 메소드
  private NodeCost getNodeCost(Node prev, Node next) throws IOException, InterruptedException {
      NodeCostParam nodeCostParam = new NodeCostParam();
      nodeCostParam.setStartNodeId(prev.getId());
      nodeCostParam.setEndNodeId(next.getId());
      NodeCost nodeCost = nodeCostService.getOneByParam(nodeCostParam);
      
      // 노드 간 이동 거리 및 시간 정보가 없으면 새로운 경로 계산
      if (nodeCost == null) {
          KakaoDirections kakaoDirections = KakaoApiUtil.getKakaoDirections(new Point(prev.getX(), prev.getY()),
              new Point(next.getX(), next.getY()));
          List<Route> routes = kakaoDirections.getRoutes();
          Route route = routes.get(0);
          
          List<Point> pathPointList = new ArrayList<Point>();
          List<Section> sections = route.getSections();
          
          // 경로가 없을 경우 처리
          if (sections == null) {
              // 경로가 없는 경우의 처리 로직
              pathPointList.add(new Point(prev.getX(), prev.getY()));
              pathPointList.add(new Point(next.getX(), next.getY()));
              nodeCost = new NodeCost();
              nodeCost.setStartNodeId(prev.getId());// 시작노드id
              nodeCost.setEndNodeId(next.getId());// 종료노드id
              nodeCost.setDistanceMeter(0L);// 이동거리(미터)
              nodeCost.setDurationSecond(0L);// 이동시간(초)
              nodeCost.setTollFare(0);// 통행 요금(톨게이트)
              nodeCost.setTaxiFare(0);// 택시 요금(지자체별, 심야, 시경계, 복합, 콜비 감안)
              nodeCost.setPathJson(new ObjectMapper().writeValueAsString(pathPointList));// 이동경로json [[x,y],[x,y]]
              nodeCost.setRegDt(new Date());// 등록일시
              nodeCost.setModDt(new Date());// 수정일시
              //DB node_cost 테이블에 데이터 저장.
              nodeCostService.add(nodeCost);
              return null;
          }
          List<Road> roads = sections.get(0).getRoads();
          for (Road road : roads) {
              List<Double> vertexes = road.getVertexes();
              for (int q = 0; q < vertexes.size(); q++) {
                  pathPointList.add(new Point(vertexes.get(q), vertexes.get(++q)));
              }
          }
          Summary summary = route.getSummary();
          Integer distance = summary.getDistance();
          Integer duration = summary.getDuration();
          Fare fare = summary.getFare();
          Integer taxi = fare.getTaxi();
          Integer toll = fare.getToll();
          nodeCost = new NodeCost();
          nodeCost.setStartNodeId(prev.getId());// 시작노드id
          nodeCost.setEndNodeId(next.getId());// 종료노드id
          nodeCost.setDistanceMeter(distance.longValue());// 이동거리(미터)
          nodeCost.setDurationSecond(duration.longValue());// 이동시간(초)
          nodeCost.setTollFare(toll);// 통행 요금(톨게이트)
          nodeCost.setTaxiFare(taxi);// 택시 요금(지자체별, 심야, 시경계, 복합, 콜비 감안)
          nodeCost.setPathJson(new ObjectMapper().writeValueAsString(pathPointList));// 이동경로json [[x,y],[x,y]]
          nodeCost.setRegDt(new Date());// 등록일시
          nodeCost.setModDt(new Date());// 수정일시
          nodeCostService.add(nodeCost);
      }
      return nodeCost; // 노드 간 이동 거리 및 시간 정보 반환
  }


  

}// class end
