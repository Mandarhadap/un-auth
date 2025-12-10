package app.api;

import app.util.DBUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeptServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<JsonObject> list = new ArrayList<>();

        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT deptno, dname, loc FROM dept ORDER BY deptno")) {

            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("deptno", rs.getInt("deptno"));
                obj.addProperty("dname", rs.getString("dname"));
                obj.addProperty("loc", rs.getString("loc"));
                list.add(obj);
            }

            String json = gson.toJson(list);
            PrintWriter out = resp.getWriter();
            out.print(json);
            out.flush();

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB error\"}");
            e.printStackTrace();
        }
    }

    // Create department: POST /api/departments
    // Body JSON: { "deptno":10, "dname":"ACCOUNTING", "loc":"NEW YORK" }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        int deptno = body.get("deptno").getAsInt();
        String dname = body.get("dname").getAsString();
        String loc = body.get("loc").getAsString();

        String sql = "INSERT INTO dept (deptno, dname, loc) VALUES (?, ?, ?)";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, deptno);
            ps.setString(2, dname);
            ps.setString(3, loc);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(gson.toJson(body));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB insert failed\"}");
            e.printStackTrace();
        }
    }

    // Delete department: DELETE /api/departments?deptno=10
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String deptnoStr = req.getParameter("deptno");
        if (deptnoStr == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"deptno parameter required\"}");
            return;
        }
        int deptno = Integer.parseInt(deptnoStr);

        String sql = "DELETE FROM dept WHERE deptno = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, deptno);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"message\":\"No such department\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB delete failed\"}");
            e.printStackTrace();
        }
    }

    // UPDATE department: PUT /api/departments
    // Body: { "deptno":10, "dname":"NEWNAME", "loc":"NEWLOC" }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
            
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
            
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JsonObject body = new Gson().fromJson(sb.toString(), JsonObject.class);
    
        int deptno = body.get("deptno").getAsInt();
        String dname = body.get("dname").getAsString();
        String loc = body.get("loc").getAsString();
    
        String sql = "UPDATE dept SET dname = ?, loc = ? WHERE deptno = ?";
    
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dname);
            ps.setString(2, loc);
            ps.setInt(3, deptno);
            
            int rows = ps.executeUpdate();
            if (rows == 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"message\":\"No such department\"}");
            } else {
                resp.getWriter().write(body.toString());
            }
        
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB update failed\"}");
            e.printStackTrace();
        }
    }

}
