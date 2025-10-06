<?php
include "db.php";

$user_email = $_GET['email'];

$sql = "SELECT * FROM antrian WHERE user_email = '$user_email' ORDER BY created_at DESC";
$result = $conn->query($sql);

$rows = [];
while ($row = $result->fetch_assoc()) {
    $rows[] = $row;
}
echo json_encode($rows);
$conn->close();
?>
