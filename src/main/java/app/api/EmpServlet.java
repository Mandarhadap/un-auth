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

public class EmpServlet extends HttpServlet {

    private final Gson gson = new Gson();

    // GET /api/employees?deptno=10  (if deptno omitted -> all employees)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String deptnoStr = req.getParameter("deptno");

        List<JsonObject> list = new ArrayList<>();

        String sql;
        boolean filterByDept = (deptnoStr != null && !deptnoStr.isEmpty());

        if (filterByDept) {
            sql = "SELECT empno, ename, job, mgr, hiredate, sal, comm, deptno " +
                  "FROM emp WHERE deptno = ? ORDER BY empno";
        } else {
            sql = "SELECT empno, ename, job, mgr, hiredate, sal, comm, deptno " +
                  "FROM emp ORDER BY empno";
        }

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filterByDept) {
                ps.setInt(1, Integer.parseInt(deptnoStr));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("empno", rs.getInt("empno"));
                    obj.addProperty("ename", rs.getString("ename"));
                    obj.addProperty("job", rs.getString("job"));
                    int mgr = rs.getInt("mgr");
                    if (!rs.wasNull()) obj.addProperty("mgr", mgr);
                    Date hiredate = rs.getDate("hiredate");
                    if (hiredate != null) obj.addProperty("hiredate", hiredate.toString());
                    double sal = rs.getDouble("sal");
                    if (!rs.wasNull()) obj.addProperty("sal", sal);
                    double comm = rs.getDouble("comm");
                    if (!rs.wasNull()) obj.addProperty("comm", comm);
                    obj.addProperty("deptno", rs.getInt("deptno"));
                    list.add(obj);
                }
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

    // POST /api/employees
    // Body: { empno, ename, job, mgr, hiredate, sal, comm, deptno }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
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
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        int empno = body.get("empno").getAsInt();
        String ename = body.get("ename").getAsString();
        String job = body.has("job") && !body.get("job").isJsonNull()
                ? body.get("job").getAsString() : null;
        Integer mgr = body.has("mgr") && !body.get("mgr").isJsonNull()
                ? body.get("mgr").getAsInt() : null;
        String hiredateStr = body.has("hiredate") && !body.get("hiredate").isJsonNull()
                ? body.get("hiredate").getAsString() : null;
        Double sal = body.has("sal") && !body.get("sal").isJsonNull()
                ? body.get("sal").getAsDouble() : null;
        Double comm = body.has("comm") && !body.get("comm").isJsonNull()
                ? body.get("comm").getAsDouble() : null;
        int deptno = body.get("deptno").getAsInt();

        String sql = "INSERT INTO emp (empno, ename, job, mgr, hiredate, sal, comm, deptno) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empno);
            ps.setString(2, ename);
            if (job != null) ps.setString(3, job); else ps.setNull(3, Types.VARCHAR);
            if (mgr != null) ps.setInt(4, mgr); else ps.setNull(4, Types.INTEGER);

            if (hiredateStr != null && !hiredateStr.isEmpty()) {
                ps.setDate(5, Date.valueOf(hiredateStr)); // yyyy-MM-dd
            } else {
                ps.setNull(5, Types.DATE);
            }

            if (sal != null) ps.setDouble(6, sal); else ps.setNull(6, Types.DOUBLE);
            if (comm != null) ps.setDouble(7, comm); else ps.setNull(7, Types.DOUBLE);
            ps.setInt(8, deptno);

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(gson.toJson(body));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB insert failed\"}");
            e.printStackTrace();
        }
    }

    // DELETE /api/employees?empno=7369
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String empnoStr = req.getParameter("empno");
        if (empnoStr == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"empno parameter required\"}");
            return;
        }
        int empno = Integer.parseInt(empnoStr);

        String sql = "DELETE FROM emp WHERE empno = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empno);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"message\":\"No such employee\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"DB delete failed\"}");
            e.printStackTrace();
        }
    }

    // UPDATE employee: PUT /api/employees
    // Body: { empno, ename, job, mgr, hiredate, sal, comm, deptno }
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
    
        int empno = body.get("empno").getAsInt();
        String ename = body.get("ename").getAsString();
        String job = body.has("job") && !body.get("job").isJsonNull()
                ? body.get("job").getAsString() : null;
        Integer mgr = body.has("mgr") && !body.get("mgr").isJsonNull()
                ? body.get("mgr").getAsInt() : null;
        String hiredateStr = body.has("hiredate") && !body.get("hiredate").isJsonNull()
                ? body.get("hiredate").getAsString() : null;
        Double sal = body.has("sal") && !body.get("sal").isJsonNull()
                ? body.get("sal").getAsDouble() : null;
        Double comm = body.has("comm") && !body.get("comm").isJsonNull()
                ? body.get("comm").getAsDouble() : null;
        int deptno = body.get("deptno").getAsInt();
    
        String sql = "UPDATE emp SET ename=?, job=?, mgr=?, hiredate=?, sal=?, comm=?, deptno=? " +
                     "WHERE empno=?";
    
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, ename);
            if (job != null) ps.setString(2, job); else ps.setNull(2, Types.VARCHAR);
            if (mgr != null) ps.setInt(3, mgr); else ps.setNull(3, Types.INTEGER);
            
            if (hiredateStr != null && !hiredateStr.isEmpty()) {
                ps.setDate(4, Date.valueOf(hiredateStr));
            } else {
                ps.setNull(4, Types.DATE);
            }
        
            if (sal != null) ps.setDouble(5, sal); else ps.setNull(5, Types.DOUBLE);
            if (comm != null) ps.setDouble(6, comm); else ps.setNull(6, Types.DOUBLE);
            ps.setInt(7, deptno);
            ps.setInt(8, empno);
        
            int rows = ps.executeUpdate();
            if (rows == 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"message\":\"No such employee\"}");
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
