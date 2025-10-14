<?php
include "db.php";

$email = $_POST['email'] ?? '';
$nama = $_POST['nama'] ?? '';
$image = $_POST['photo'] ?? '';

if ($email == '' || $nama == '') {
    echo json_encode(["success" => false, "message" => "Data tidak lengkap"]);
    exit;
}

// update name first
$sql = $conn->prepare("UPDATE users SET nama=? WHERE email=?");
$sql->bind_param("ss", $nama, $email);
$sql->execute();

// handle photo if sent
if ($image != '') {
    $imageData = base64_decode($image);
    $fileName = uniqid() . ".jpg";
    $filePath = "uploads/" . $fileName;

    if (file_put_contents($filePath, $imageData)) {
        $sql = $conn->prepare("UPDATE users SET photo=? WHERE email=?");
        $sql->bind_param("ss", $fileName, $email);
        $sql->execute();
    }
}

echo json_encode(["success" => true, "message" => "Profile updated"]);
?>
