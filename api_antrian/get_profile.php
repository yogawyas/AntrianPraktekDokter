<?php
include "db.php";

$email = $_POST['email'] ?? '';

if ($email == '') {
    echo json_encode(["success" => false, "message" => "Email tidak dikirim"]);
    exit;
}

$sql = $conn->prepare("SELECT nama, email, photo FROM users WHERE email = ?");
$sql->bind_param("s", $email);
$sql->execute();
$result = $sql->get_result();

if ($row = $result->fetch_assoc()) {
    echo json_encode([
        "success" => true,
        "nama" => $row['nama'],
        "email" => $row['email'],
        "photo" => $row['photo']
    ]);
} else {
    echo json_encode(["success" => false, "message" => "User tidak ditemukan"]);
}
?>